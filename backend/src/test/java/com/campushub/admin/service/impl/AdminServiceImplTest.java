package com.campushub.admin.service.impl;

import com.campushub.common.exception.BusinessException;
import com.campushub.entity.BizRequirement;
import com.campushub.entity.SysUser;
import com.campushub.mapper.BizRequirementMapper;
import com.campushub.mapper.SysUserMapper;
import com.campushub.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private BizRequirementMapper bizRequirementMapper;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AdminServiceImpl adminService;

    private SysUser adminUser;
    private SysUser normalUser;

    @BeforeEach
    void setUp() {
        adminUser = new SysUser();
        adminUser.setUserId(1L);
        adminUser.setRole(1);

        normalUser = new SysUser();
        normalUser.setUserId(2L);
        normalUser.setRole(0);
    }

    // ==================== cancelRequirement ====================

    @Test
    void cancelRequirement_ShouldSucceed_WhenAdmin() {
        BizRequirement req = new BizRequirement();
        req.setReqId(100L);
        req.setPublisherId(200L);
        req.setTitle("违规需求");

        when(sysUserMapper.selectById(1L)).thenReturn(adminUser);
        when(bizRequirementMapper.selectById(100L)).thenReturn(req);

        adminService.cancelRequirement(100L, 1L);

        assertEquals("CANCELED", req.getStatus());
        verify(bizRequirementMapper).updateById(ArgumentMatchers.<BizRequirement>argThat(r ->
                "CANCELED".equals(r.getStatus())));
        verify(notificationService).createNotification(
                eq(200L), contains("下架"), anyString(), eq("ADMIN_REQUIREMENT_CANCELED"));
    }

    @Test
    void cancelRequirement_ShouldThrow_WhenNotAdmin() {
        when(sysUserMapper.selectById(2L)).thenReturn(normalUser);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminService.cancelRequirement(100L, 2L));
        assertEquals(403, ex.getCode());
        verify(bizRequirementMapper, never()).updateById(ArgumentMatchers.<BizRequirement>any());
    }

    @Test
    void cancelRequirement_ShouldThrow_WhenUserNotFound() {
        when(sysUserMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminService.cancelRequirement(100L, 999L));
        assertEquals(403, ex.getCode());
    }

    @Test
    void cancelRequirement_ShouldThrow_WhenRequirementNotFound() {
        when(sysUserMapper.selectById(1L)).thenReturn(adminUser);
        when(bizRequirementMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminService.cancelRequirement(999L, 1L));
        assertEquals(404, ex.getCode());
    }

    @Test
    void cancelRequirement_ShouldThrow_WhenRoleIsNull() {
        SysUser nullRoleUser = new SysUser();
        nullRoleUser.setUserId(3L);
        nullRoleUser.setRole(null);
        when(sysUserMapper.selectById(3L)).thenReturn(nullRoleUser);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminService.cancelRequirement(100L, 3L));
        assertEquals(403, ex.getCode());
    }
}
