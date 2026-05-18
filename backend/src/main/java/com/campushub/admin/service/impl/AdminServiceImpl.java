package com.campushub.admin.service.impl;

import com.campushub.admin.service.AdminService;
import com.campushub.common.exception.BusinessException;
import com.campushub.entity.BizRequirement;
import com.campushub.entity.SysUser;
import com.campushub.mapper.BizRequirementMapper;
import com.campushub.mapper.SysUserMapper;
import com.campushub.notification.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private BizRequirementMapper bizRequirementMapper;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private NotificationService notificationService;

    @Override
    public void cancelRequirement(Long reqId, Long adminId) {
        requireAdmin(adminId);

        // 2. 查询目标需求
        BizRequirement req = bizRequirementMapper.selectById(reqId);
        if (req == null) {
            throw new BusinessException(404, "目标需求不存在");
        }

        // 3. 执行下架（修改状态为 CANCELED）
        req.setStatus("CANCELED");
        bizRequirementMapper.updateById(req);
        notificationService.createNotification(
                req.getPublisherId(),
                "需求已被管理员下架",
                "你的需求“" + req.getTitle() + "”因违规或过期已被下架。",
                "ADMIN_REQUIREMENT_CANCELED"
        );
    }

    @Override
    public List<Map<String, Object>> listUsers(Long adminId) {
        requireAdmin(adminId);
        return jdbcClient.sql("""
                        SELECT user_id, username, nickname, campus, credit_score, role, created_at
                        FROM sys_user
                        ORDER BY created_at DESC
                        LIMIT 100
                        """)
                .query()
                .listOfRows();
    }

    @Override
    public List<Map<String, Object>> listRequirements(Long adminId) {
        requireAdmin(adminId);
        return jdbcClient.sql("""
                        SELECT r.req_id, r.title, r.type, r.status, r.budget, r.created_at,
                               r.publisher_id, COALESCE(u.nickname, u.username) AS publisher_name
                        FROM biz_requirement r
                        LEFT JOIN sys_user u ON r.publisher_id = u.user_id
                        ORDER BY r.created_at DESC
                        LIMIT 100
                        """)
                .query()
                .listOfRows();
    }

    @Override
    public List<Map<String, Object>> listOrders(Long adminId) {
        requireAdmin(adminId);
        return jdbcClient.sql("""
                        SELECT o.order_id, o.req_id, r.title AS req_title, o.receiver_id,
                               o.amount, o.status, o.created_at, o.finished_at
                        FROM biz_order o
                        JOIN biz_requirement r ON o.req_id = r.req_id
                        ORDER BY o.created_at DESC
                        LIMIT 100
                        """)
                .query()
                .listOfRows();
    }

    private void requireAdmin(Long adminId) {
        // 统一管理员校验，避免每个后台接口散落重复权限判断。
        SysUser user = sysUserMapper.selectById(adminId);
        if (user == null || user.getRole() == null || user.getRole() != 1) {
            throw new BusinessException(403, "越权操作：仅系统管理员可执行此操作");
        }
    }
}
