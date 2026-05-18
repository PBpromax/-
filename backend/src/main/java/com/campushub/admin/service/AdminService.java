package com.campushub.admin.service;

import java.util.List;
import java.util.Map;

public interface AdminService {
    /**
     * 下架(取消)违规需求
     */
    void cancelRequirement(Long reqId, Long adminId);

    List<Map<String, Object>> listUsers(Long adminId);

    List<Map<String, Object>> listRequirements(Long adminId);

    List<Map<String, Object>> listOrders(Long adminId);
}
