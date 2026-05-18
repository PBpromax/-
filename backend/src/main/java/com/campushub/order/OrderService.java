package com.campushub.order;

import com.campushub.common.exception.ApiCode;
import com.campushub.common.exception.BusinessException;
import com.campushub.entity.BizOrder;
import com.campushub.entity.BizRequirement;
import com.campushub.entity.OrderStatus;
import com.campushub.mapper.BizOrderMapper;
import com.campushub.mapper.BizRequirementMapper;
import com.campushub.notification.NotificationService;
import com.campushub.order.dto.OrderDetailResponse;
import com.campushub.order.dto.OrderListItem;
import com.campushub.requirement.dto.PageResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {
    private final JdbcClient jdbcClient;
    private final BizOrderMapper bizOrderMapper;
    private final BizRequirementMapper bizRequirementMapper;
    private final NotificationService notificationService;

    public OrderService(JdbcClient jdbcClient, BizOrderMapper bizOrderMapper,
                        BizRequirementMapper bizRequirementMapper,
                        NotificationService notificationService) {
        this.jdbcClient = jdbcClient;
        this.bizOrderMapper = bizOrderMapper;
        this.bizRequirementMapper = bizRequirementMapper;
        this.notificationService = notificationService;
    }

    @Transactional
    public Long createOrder(Long reqId, Long receiverId) {
        var row = jdbcClient.sql("""
                        SELECT publisher_id, budget, status
                        FROM biz_requirement WHERE req_id = :reqId
                        """)
                .param("reqId", reqId)
                .query((rs, rowNum) -> new Object[]{
                        rs.getLong("publisher_id"),
                        rs.getBigDecimal("budget"),
                        rs.getString("status")
                })
                .optional()
                .orElseThrow(() -> new BusinessException(404, "需求不存在"));

        Long publisherId = (Long) row[0];
        BigDecimal budget = (BigDecimal) row[1];
        String status = (String) row[2];

        if (publisherId.equals(receiverId)) {
            throw new BusinessException(ApiCode.SELF_ORDER);
        }
        if (!"PENDING".equals(status)) {
            throw new BusinessException(ApiCode.ORDER_TAKEN);
        }

        BizOrder order = new BizOrder();
        order.setReqId(reqId);
        order.setReceiverId(receiverId);
        order.setAmount(budget);
        order.setStatus("IN_PROGRESS");

        try {
            bizOrderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ApiCode.ORDER_TAKEN);
        }

        BizRequirement req = new BizRequirement();
        req.setReqId(reqId);
        req.setStatus("ACCEPTED");
        bizRequirementMapper.updateById(req);

        notificationService.createNotification(
                publisherId,
                "你的需求已被接单",
                "需求 " + reqId + " 已被用户 " + receiverId + " 接单，请及时跟进。",
                "ORDER_ACCEPTED"
        );
        notificationService.createNotification(
                receiverId,
                "接单成功",
                "你已成功接取需求 " + reqId + "，请按约定完成任务。",
                "ORDER_ACCEPTED"
        );

        return order.getOrderId();
    }

    @Transactional
    public OrderDetailResponse changeOrderStatus(Long orderId, Long userId, String event) {
        var context = jdbcClient.sql("""
                        SELECT o.order_id, o.req_id, o.receiver_id, o.status, r.publisher_id
                        FROM biz_order o
                        JOIN biz_requirement r ON o.req_id = r.req_id
                        WHERE o.order_id = :orderId
                        """)
                .param("orderId", orderId)
                .query((rs, rowNum) -> new OrderStateContext(
                        rs.getLong("order_id"),
                        rs.getLong("req_id"),
                        rs.getLong("publisher_id"),
                        rs.getLong("receiver_id"),
                        rs.getString("status")
                ))
                .optional()
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));

        if (!context.publisherId().equals(userId) && !context.receiverId().equals(userId)) {
            throw new BusinessException(ApiCode.FORBIDDEN);
        }

        String normalizedEvent = event == null ? "" : event.trim().toUpperCase();
        String nextStatus = resolveNextStatus(context.status(), normalizedEvent, userId,
                context.publisherId(), context.receiverId());

        BizOrder order = new BizOrder();
        order.setOrderId(orderId);
        order.setStatus(nextStatus);
        if (OrderStatus.COMPLETED.name().equals(nextStatus)) {
            order.setFinishedAt(LocalDateTime.now());
        }
        bizOrderMapper.updateById(order);

        if (OrderStatus.COMPLETED.name().equals(nextStatus)
                || OrderStatus.CANCELED.name().equals(nextStatus)) {
            BizRequirement requirement = new BizRequirement();
            requirement.setReqId(context.reqId());
            requirement.setStatus(nextStatus);
            bizRequirementMapper.updateById(requirement);
        }

        notifyStatusChange(context, nextStatus);
        return getOrderDetail(orderId, userId);
    }

    private String resolveNextStatus(String currentStatus, String event, Long operatorId,
                                     Long publisherId, Long receiverId) {
        return switch (event) {
            case "SUBMIT" -> {
                if (!receiverId.equals(operatorId) || !OrderStatus.IN_PROGRESS.name().equals(currentStatus)) {
                    throw new BusinessException(400, "当前状态不允许提交验收");
                }
                yield OrderStatus.TO_CONFIRM.name();
            }
            case "CONFIRM" -> {
                if (!publisherId.equals(operatorId) || !OrderStatus.TO_CONFIRM.name().equals(currentStatus)) {
                    throw new BusinessException(400, "当前状态不允许确认完成");
                }
                yield OrderStatus.COMPLETED.name();
            }
            case "CANCEL" -> {
                if (OrderStatus.COMPLETED.name().equals(currentStatus)
                        || OrderStatus.CANCELED.name().equals(currentStatus)) {
                    throw new BusinessException(400, "终态订单不可取消");
                }
                yield OrderStatus.CANCELED.name();
            }
            default -> throw new BusinessException(400, "未知订单事件：" + event);
        };
    }

    private void notifyStatusChange(OrderStateContext context, String nextStatus) {
        String title = "订单状态已更新";
        String content = "订单 " + context.orderId() + " 当前状态为 " + nextStatus + "。";
        notificationService.createNotification(context.publisherId(), title, content, "ORDER_STATUS_CHANGED");
        notificationService.createNotification(context.receiverId(), title, content, "ORDER_STATUS_CHANGED");
    }

    public OrderDetailResponse getOrderDetail(Long orderId, Long userId) {
        var row = jdbcClient.sql("""
                        SELECT o.order_id, o.req_id, r.title AS req_title,
                               r.description AS req_description,
                               r.publisher_id,
                               COALESCE(pu.nickname, pu.username) AS publisher_name,
                               o.receiver_id,
                               COALESCE(ru.nickname, ru.username) AS receiver_name,
                               o.amount, o.status, o.created_at, o.finished_at
                        FROM biz_order o
                        JOIN biz_requirement r ON o.req_id = r.req_id
                        JOIN sys_user pu ON r.publisher_id = pu.user_id
                        JOIN sys_user ru ON o.receiver_id = ru.user_id
                        WHERE o.order_id = :orderId
                        """)
                .param("orderId", orderId)
                .query((rs, rowNum) -> new OrderDetailResponse(
                        rs.getLong("order_id"),
                        rs.getLong("req_id"),
                        rs.getString("req_title"),
                        rs.getString("req_description"),
                        rs.getLong("publisher_id"),
                        rs.getString("publisher_name"),
                        rs.getLong("receiver_id"),
                        rs.getString("receiver_name"),
                        rs.getBigDecimal("amount"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("finished_at") != null
                                ? rs.getTimestamp("finished_at").toLocalDateTime() : null
                ))
                .optional()
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));

        if (!row.publisherId().equals(userId) && !row.receiverId().equals(userId)) {
            throw new BusinessException(ApiCode.FORBIDDEN);
        }
        return row;
    }

    public PageResponse<OrderListItem> listRelatedOrders(String tab, Long userId,
                                                          Integer page, Integer pageSize) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null ? 10 : Math.min(Math.max(pageSize, 1), 50);
        int offset = (safePage - 1) * safePageSize;

        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);

        if ("published".equals(tab)) {
            Long total = jdbcClient.sql(
                            "SELECT COUNT(*) FROM biz_requirement WHERE publisher_id = :userId")
                    .param("userId", userId)
                    .query(Long.class)
                    .single();

            params.put("limit", safePageSize);
            params.put("offset", offset);
            // 直接查需求表，LEFT JOIN 订单和接单者，未接单也能显示
            List<OrderListItem> list = jdbcClient.sql("""
                            SELECT r.req_id, r.title AS req_title, r.budget, r.status AS req_status,
                                   o.order_id, o.receiver_id, o.amount, o.status AS order_status,
                                   o.created_at AS order_created_at, r.created_at AS req_created_at,
                                   COALESCE(ru.nickname, ru.username) AS receiver_name
                            FROM biz_requirement r
                            LEFT JOIN biz_order o ON r.req_id = o.req_id
                            LEFT JOIN sys_user ru ON o.receiver_id = ru.user_id
                            WHERE r.publisher_id = :userId
                            ORDER BY r.created_at DESC
                            LIMIT :limit OFFSET :offset""")
                    .params(params)
                    .query((rs, rowNum) -> new OrderListItem(
                            rs.getObject("order_id") != null
                                    ? rs.getLong("order_id") : null,
                            rs.getLong("req_id"),
                            rs.getString("req_title"),
                            rs.getObject("receiver_id") != null
                                    ? rs.getLong("receiver_id") : null,
                            rs.getString("receiver_name"),
                            rs.getBigDecimal("amount") != null
                                    ? rs.getBigDecimal("amount") : rs.getBigDecimal("budget"),
                            rs.getString("order_status") != null
                                    ? rs.getString("order_status") : rs.getString("req_status"),
                            rs.getTimestamp("order_created_at") != null
                                    ? rs.getTimestamp("order_created_at").toLocalDateTime()
                                    : rs.getTimestamp("req_created_at").toLocalDateTime()
                    ))
                    .list();

            return new PageResponse<>(total == null ? 0 : total, safePage, safePageSize, list);
        }

        // "received" tab: 查 biz_order WHERE receiver_id = userId
        Long total = jdbcClient.sql(
                        "SELECT COUNT(*) FROM biz_order WHERE receiver_id = :userId")
                .param("userId", userId)
                .query(Long.class)
                .single();

        params.put("limit", safePageSize);
        params.put("offset", offset);
        List<OrderListItem> list = jdbcClient.sql("""
                        SELECT o.order_id, o.req_id, r.title AS req_title,
                               o.receiver_id,
                               COALESCE(ru.nickname, ru.username) AS receiver_name,
                               o.amount, o.status, o.created_at
                        FROM biz_order o
                        JOIN biz_requirement r ON o.req_id = r.req_id
                        JOIN sys_user ru ON o.receiver_id = ru.user_id
                        WHERE o.receiver_id = :userId
                        ORDER BY o.created_at DESC
                        LIMIT :limit OFFSET :offset""")
                .params(params)
                .query((rs, rowNum) -> new OrderListItem(
                        rs.getLong("order_id"),
                        rs.getLong("req_id"),
                        rs.getString("req_title"),
                        rs.getLong("receiver_id"),
                        rs.getString("receiver_name"),
                        rs.getBigDecimal("amount"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ))
                .list();

        return new PageResponse<>(total == null ? 0 : total, safePage, safePageSize, list);
    }

    private record OrderStateContext(
            Long orderId,
            Long reqId,
            Long publisherId,
            Long receiverId,
            String status
    ) {
    }
}
