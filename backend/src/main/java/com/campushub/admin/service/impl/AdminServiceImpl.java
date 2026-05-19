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
                        SELECT user_id, username, nickname, student_id, campus, college, major, grade,
                               bio, contact_visible, credit_score, role, created_at
                        FROM sys_user
                        ORDER BY created_at DESC
                        LIMIT 100
                        """)
                .query()
                .listOfRows();
    }

    @Override
    public void updateUser(Long adminId, Long userId, Map<String, Object> updates) {
        requireAdmin(adminId);
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setNickname(asString(updates.get("nickname")));
        user.setStudentId(asString(updates.get("studentId")));
        user.setCampus(asString(updates.get("campus")));
        user.setCollege(asString(updates.get("college")));
        user.setMajor(asString(updates.get("major")));
        user.setGrade(asString(updates.get("grade")));
        user.setBio(asString(updates.get("bio")));
        user.setContactVisible(asBoolean(updates.get("contactVisible")));
        user.setCreditScore(clamp(asInteger(updates.get("creditScore"), user.getCreditScore()), 0, 100));
        user.setRole("admin_test".equals(user.getUsername()) ? 1 : 0);
        sysUserMapper.updateById(user);
    }

    @Override
    public void deleteUser(Long adminId, Long userId) {
        requireAdmin(adminId);
        if (adminId.equals(userId)) {
            throw new BusinessException(400, "不能删除当前登录管理员");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if ("admin_test".equals(user.getUsername())) {
            throw new BusinessException(400, "admin_test 管理员账号必须保留");
        }
        deleteUserRelatedData(userId);
        sysUserMapper.deleteById(userId);
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

    private void deleteUserRelatedData(Long userId) {
        jdbcClient.sql("""
                        DELETE FROM biz_evaluation
                        WHERE reviewer_id = :userId OR target_id = :userId
                           OR order_id IN (
                               SELECT o.order_id
                               FROM biz_order o
                               JOIN biz_requirement r ON o.req_id = r.req_id
                               WHERE o.receiver_id = :userId OR r.publisher_id = :userId
                           )
                        """)
                .param("userId", userId)
                .update();
        jdbcClient.sql("""
                        DELETE FROM biz_order
                        WHERE receiver_id = :userId
                           OR req_id IN (SELECT req_id FROM biz_requirement WHERE publisher_id = :userId)
                        """)
                .param("userId", userId)
                .update();
        jdbcClient.sql("DELETE FROM biz_requirement WHERE publisher_id = :userId")
                .param("userId", userId)
                .update();
        jdbcClient.sql("DELETE FROM biz_notification WHERE user_id = :userId")
                .param("userId", userId)
                .update();
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private Boolean asBoolean(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private Integer asInteger(Object value, Integer fallback) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private int clamp(Integer value, int min, int max) {
        int safeValue = value == null ? min : value;
        return Math.max(min, Math.min(max, safeValue));
    }
}
