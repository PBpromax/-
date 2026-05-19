package com.campushub.admin.controller;

import com.campushub.admin.service.AdminService;
import com.campushub.common.api.ApiResponse;
import com.campushub.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private CurrentUser currentUser;

    @PutMapping("/requirements/{reqId}/cancel")
    public ApiResponse<Void> cancelRequirement(
            @PathVariable Long reqId,
            HttpServletRequest request) {
        
        // 利用安全工具获取当前用户 ID
        Long adminId = currentUser.requireUserId(request);
        
        adminService.cancelRequirement(reqId, adminId);
        return ApiResponse.success("违规需求已成功下架", null);
    }

    @GetMapping("/users")
    public ApiResponse<List<Map<String, Object>>> listUsers(HttpServletRequest request) {
        Long adminId = currentUser.requireUserId(request);
        return ApiResponse.success(adminService.listUsers(adminId));
    }

    @PutMapping("/users/{userId}")
    public ApiResponse<Void> updateUser(
            HttpServletRequest request,
            @PathVariable Long userId,
            @RequestBody Map<String, Object> updates) {
        Long adminId = currentUser.requireUserId(request);
        adminService.updateUser(adminId, userId, updates);
        return ApiResponse.success("用户信息已更新", null);
    }

    @DeleteMapping("/users/{userId}")
    public ApiResponse<Void> deleteUser(HttpServletRequest request, @PathVariable Long userId) {
        Long adminId = currentUser.requireUserId(request);
        adminService.deleteUser(adminId, userId);
        return ApiResponse.success("用户已删除", null);
    }

    @GetMapping("/requirements")
    public ApiResponse<List<Map<String, Object>>> listRequirements(HttpServletRequest request) {
        Long adminId = currentUser.requireUserId(request);
        return ApiResponse.success(adminService.listRequirements(adminId));
    }

    @GetMapping("/orders")
    public ApiResponse<List<Map<String, Object>>> listOrders(HttpServletRequest request) {
        Long adminId = currentUser.requireUserId(request);
        return ApiResponse.success(adminService.listOrders(adminId));
    }
}
