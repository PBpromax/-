package com.campushub.order;

import com.campushub.common.exception.BusinessException;
import com.campushub.entity.OrderStatus;
import com.campushub.order.dto.OrderDetailResponse;
import com.campushub.order.dto.OrderListItem;
import com.campushub.requirement.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private JdbcClient jdbcClient;

    private static final Long PUBLISHER_ID = 100L;
    private static final Long RECEIVER_ID = 200L;
    private static final Long OTHER_USER_ID = 300L;
    private static final Long PENDING_REQ_ID = 1000L;
    private static final Long ACCEPTED_REQ_ID = 1001L;
    private static final Long EXISTING_ORDER_ID = 10000L;

    @BeforeEach
    void setUp() {
        // Clean all data first
        jdbcClient.sql("DELETE FROM biz_evaluation").update();
        jdbcClient.sql("DELETE FROM biz_notification").update();
        jdbcClient.sql("DELETE FROM biz_order").update();
        jdbcClient.sql("DELETE FROM biz_requirement").update();
        jdbcClient.sql("DELETE FROM sys_user").update();

        // Insert test users
        jdbcClient.sql("""
                INSERT INTO sys_user(user_id, username, nickname, password_hash, phone_encrypted, student_id, campus, credit_score, role)
                VALUES (100, 'publisher', '发布者', 'hash', 'enc', 'S001', '南校区', 100, 0)""").update();
        jdbcClient.sql("""
                INSERT INTO sys_user(user_id, username, nickname, password_hash, phone_encrypted, student_id, campus, credit_score, role)
                VALUES (200, 'receiver', '接单人', 'hash', 'enc', 'S002', '北校区', 100, 0)""").update();
        jdbcClient.sql("""
                INSERT INTO sys_user(user_id, username, nickname, password_hash, phone_encrypted, student_id, campus, credit_score, role)
                VALUES (300, 'other', '其他人', 'hash', 'enc', 'S003', '东校区', 100, 0)""").update();

        // Insert test requirements
        jdbcClient.sql("""
                INSERT INTO biz_requirement(req_id, publisher_id, title, description, budget, type, status)
                VALUES (1000, 100, '待接需求', '描述', 50.00, 'TUTORING', 'PENDING')""").update();
        jdbcClient.sql("""
                INSERT INTO biz_requirement(req_id, publisher_id, title, description, budget, type, status)
                VALUES (1001, 100, '已接需求', '描述', 30.00, 'EXPRESS', 'ACCEPTED')""").update();
    }

    // ==================== createOrder ====================

    @Test
    void createOrder_ShouldSucceed() {
        Long orderId = orderService.createOrder(PENDING_REQ_ID, RECEIVER_ID);

        assertNotNull(orderId);

        // Verify order created
        var order = jdbcClient.sql("SELECT * FROM biz_order WHERE order_id = :id")
                .param("id", orderId)
                .query((rs, __) -> rs.getString("status"))
                .single();
        assertEquals("IN_PROGRESS", order);

        // Verify requirement status updated
        var reqStatus = jdbcClient.sql("SELECT status FROM biz_requirement WHERE req_id = :id")
                .param("id", PENDING_REQ_ID)
                .query(String.class)
                .single();
        assertEquals("ACCEPTED", reqStatus);
    }

    @Test
    void createOrder_ShouldThrow_WhenRequirementNotFound() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.createOrder(9999L, RECEIVER_ID));
        assertEquals(404, ex.getCode());
    }

    @Test
    void createOrder_ShouldThrow_WhenSelfOrder() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.createOrder(PENDING_REQ_ID, PUBLISHER_ID));
        assertEquals(4002, ex.getCode());
    }

    @Test
    void createOrder_ShouldThrow_WhenAlreadyAccepted() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.createOrder(ACCEPTED_REQ_ID, RECEIVER_ID));
        assertEquals(4001, ex.getCode());
    }

    @Test
    void createOrder_ShouldThrow_WhenDuplicateOrder() {
        // First order succeeds
        orderService.createOrder(PENDING_REQ_ID, RECEIVER_ID);

        // Second order on same req should fail (uk_req_id constraint)
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.createOrder(PENDING_REQ_ID, OTHER_USER_ID));
        assertEquals(4001, ex.getCode());
    }

    // ==================== changeOrderStatus ====================

    private void setupOrder(String status) {
        jdbcClient.sql("""
                INSERT INTO biz_order(order_id, req_id, receiver_id, amount, status)
                VALUES (?, ?, ?, 50.00, ?)""")
                .param(1, EXISTING_ORDER_ID)
                .param(2, PENDING_REQ_ID)
                .param(3, RECEIVER_ID)
                .param(4, status)
                .update();
        jdbcClient.sql("UPDATE biz_requirement SET status = 'ACCEPTED' WHERE req_id = ?")
                .param(1, PENDING_REQ_ID)
                .update();
    }

    @Test
    void changeOrderStatus_Submit_ShouldMoveToToConfirm() {
        setupOrder("IN_PROGRESS");

        OrderDetailResponse resp = orderService.changeOrderStatus(EXISTING_ORDER_ID, RECEIVER_ID, "SUBMIT");

        assertEquals(OrderStatus.TO_CONFIRM.name(), resp.status());
    }

    @Test
    void changeOrderStatus_Confirm_ShouldMoveToCompleted() {
        setupOrder("TO_CONFIRM");

        OrderDetailResponse resp = orderService.changeOrderStatus(EXISTING_ORDER_ID, PUBLISHER_ID, "CONFIRM");

        assertEquals(OrderStatus.COMPLETED.name(), resp.status());
        assertNotNull(resp.finishedAt(), "完成订单应有 finishedAt");

        // Requirement should also be COMPLETED
        var reqStatus = jdbcClient.sql("SELECT status FROM biz_requirement WHERE req_id = :id")
                .param("id", PENDING_REQ_ID)
                .query(String.class)
                .single();
        assertEquals("COMPLETED", reqStatus);
    }

    @Test
    void changeOrderStatus_Cancel_ShouldMoveToCanceled() {
        setupOrder("IN_PROGRESS");

        OrderDetailResponse resp = orderService.changeOrderStatus(EXISTING_ORDER_ID, PUBLISHER_ID, "CANCEL");

        assertEquals(OrderStatus.CANCELED.name(), resp.status());
    }

    @Test
    void changeOrderStatus_CancelCompleted_ShouldThrow() {
        setupOrder("COMPLETED");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.changeOrderStatus(EXISTING_ORDER_ID, PUBLISHER_ID, "CANCEL"));
        assertTrue(ex.getMessage().contains("终态"));
    }

    @Test
    void changeOrderStatus_Unauthorized_ShouldThrow() {
        setupOrder("IN_PROGRESS");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.changeOrderStatus(EXISTING_ORDER_ID, OTHER_USER_ID, "SUBMIT"));
        assertEquals(403, ex.getCode());
    }

    @Test
    void changeOrderStatus_InvalidTransition_ShouldThrow() {
        setupOrder("IN_PROGRESS");

        // CONFIRM requires TO_CONFIRM state, not IN_PROGRESS
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.changeOrderStatus(EXISTING_ORDER_ID, PUBLISHER_ID, "CONFIRM"));
        assertEquals(400, ex.getCode());
    }

    @Test
    void changeOrderStatus_SubmitByPublisher_ShouldThrow() {
        setupOrder("IN_PROGRESS");

        // Only receiver can SUBMIT
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.changeOrderStatus(EXISTING_ORDER_ID, PUBLISHER_ID, "SUBMIT"));
        assertEquals(400, ex.getCode());
    }

    @Test
    void changeOrderStatus_OrderNotFound_ShouldThrow() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.changeOrderStatus(9999L, PUBLISHER_ID, "SUBMIT"));
        assertEquals(404, ex.getCode());
    }

    // ==================== getOrderDetail ====================

    @Test
    void getOrderDetail_ShouldReturnDetail() {
        setupOrder("IN_PROGRESS");

        OrderDetailResponse detail = orderService.getOrderDetail(EXISTING_ORDER_ID, PUBLISHER_ID);

        assertEquals(EXISTING_ORDER_ID, detail.orderId());
        assertEquals(PENDING_REQ_ID, detail.reqId());
        assertEquals(PUBLISHER_ID, detail.publisherId());
        assertEquals(RECEIVER_ID, detail.receiverId());
        assertEquals("IN_PROGRESS", detail.status());
    }

    @Test
    void getOrderDetail_ShouldThrow_WhenUnauthorized() {
        setupOrder("IN_PROGRESS");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.getOrderDetail(EXISTING_ORDER_ID, OTHER_USER_ID));
        assertEquals(403, ex.getCode());
    }

    @Test
    void getOrderDetail_ShouldThrow_WhenNotFound() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.getOrderDetail(9999L, PUBLISHER_ID));
        assertEquals(404, ex.getCode());
    }

    // ==================== listRelatedOrders ====================

    @Test
    void listRelatedOrders_Received_ShouldReturnList() {
        setupOrder("IN_PROGRESS");

        PageResponse<OrderListItem> page = orderService.listRelatedOrders("received", RECEIVER_ID, 1, 10);

        assertTrue(page.total() > 0);
        assertEquals(EXISTING_ORDER_ID, page.list().get(0).orderId());
    }

    @Test
    void listRelatedOrders_Published_ShouldReturnList() {
        setupOrder("IN_PROGRESS");

        PageResponse<OrderListItem> page = orderService.listRelatedOrders("published", PUBLISHER_ID, 1, 10);

        assertTrue(page.total() >= 2, "发布者应有至少2条已发布的需求");
        // Both PENDING_REQ_ID and ACCEPTED_REQ_ID belong to publisher
        var reqIds = page.list().stream().map(OrderListItem::reqId).toList();
        assertTrue(reqIds.contains(PENDING_REQ_ID), "应包含已创建订单的需求");
        assertTrue(reqIds.contains(ACCEPTED_REQ_ID), "应包含已接单状态的需求");
    }

    @Test
    void listRelatedOrders_Empty_ShouldReturnZero() {
        PageResponse<OrderListItem> page = orderService.listRelatedOrders("received", 999L, 1, 10);
        assertEquals(0, page.total());
    }

    @Test
    void listRelatedOrders_Pagination_ShouldWork() {
        // Insert 5 orders for pagination test
        for (int i = 0; i < 5; i++) {
            Long reqId = 2000L + i;
            jdbcClient.sql("""
                    INSERT INTO biz_requirement(req_id, publisher_id, title, description, budget, type, status)
                    VALUES (?, 100, 'title', 'desc', 10.00, 'TUTORING', 'ACCEPTED')""")
                    .param(1, reqId).update();
            jdbcClient.sql("""
                    INSERT INTO biz_order(order_id, req_id, receiver_id, amount, status)
                    VALUES (?, ?, 200, 10.00, 'IN_PROGRESS')""")
                    .param(1, 20000L + i).param(2, reqId).update();
        }

        PageResponse<OrderListItem> page1 = orderService.listRelatedOrders("received", RECEIVER_ID, 1, 3);
        assertEquals(3, page1.list().size());

        PageResponse<OrderListItem> page2 = orderService.listRelatedOrders("received", RECEIVER_ID, 2, 3);
        assertTrue(page2.list().size() >= 2);
    }
}
