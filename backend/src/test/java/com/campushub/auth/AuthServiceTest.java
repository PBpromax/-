package com.campushub.auth;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campushub.auth.dto.LoginRequest;
import com.campushub.auth.dto.LoginResponse;
import com.campushub.auth.dto.RegisterRequest;
import com.campushub.common.exception.ApiCode;
import com.campushub.common.exception.BusinessException;
import com.campushub.entity.SysUser;
import com.campushub.mapper.SysUserMapper;
import com.campushub.notification.NotificationService;
import com.campushub.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AuthService authService;

    // ==================== register ====================

    @Test
    void register_ShouldSucceed_WhenUsernameNotTaken() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("newuser");
        req.setPassword("pass123");
        req.setStudentId("S2024001");
        req.setCampus("南校区");

        when(sysUserMapper.exists(any(QueryWrapper.class))).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("$hashed$");

        authService.register(req);

        verify(sysUserMapper).insert(ArgumentMatchers.<SysUser>argThat(user ->
                "newuser".equals(user.getUsername()) &&
                "$hashed$".equals(user.getPasswordHash()) &&
                "S2024001".equals(user.getStudentId()) &&
                "南校区".equals(user.getCampus()) &&
                Integer.valueOf(100).equals(user.getCreditScore())
        ));
        verify(notificationService).createNotification(
                isNull(), eq("欢迎加入 CampusHub"), anyString(), eq("AUTH_REGISTER"));
    }

    @Test
    void register_ShouldUseDefaultStudentId_WhenNotProvided() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("noid");
        req.setPassword("pass");

        when(sysUserMapper.exists(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");

        authService.register(req);

        verify(sysUserMapper).insert(ArgumentMatchers.<SysUser>argThat(user ->
                "U_noid".equals(user.getStudentId())));
    }

    @Test
    void register_ShouldThrow_WhenUsernameTaken() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("taken");
        req.setPassword("pass");

        when(sysUserMapper.exists(any(QueryWrapper.class))).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.register(req));
        assertEquals(ApiCode.USERNAME_TAKEN.getCode(), ex.getCode());
        verify(sysUserMapper, never()).insert(ArgumentMatchers.<SysUser>any());
    }

    // ==================== login ====================

    @Test
    void login_ShouldReturnToken_WhenCredentialsCorrect() {
        LoginRequest req = new LoginRequest();
        req.setUsername("user");
        req.setPassword("correct");

        SysUser dbUser = new SysUser();
        dbUser.setUserId(42L);
        dbUser.setUsername("user");
        dbUser.setPasswordHash("$hash$");

        when(sysUserMapper.selectOne(any(QueryWrapper.class))).thenReturn(dbUser);
        when(passwordEncoder.matches("correct", "$hash$")).thenReturn(true);
        when(jwtUtil.generateToken(42L)).thenReturn("jwt.token.here");

        LoginResponse resp = authService.login(req);

        assertEquals(42L, resp.getUserId());
        assertEquals("jwt.token.here", resp.getToken());
    }

    @Test
    void login_ShouldThrow_WhenUserNotFound() {
        LoginRequest req = new LoginRequest();
        req.setUsername("ghost");
        req.setPassword("any");

        when(sysUserMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(req));
        assertEquals(ApiCode.USER_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void login_ShouldThrow_WhenPasswordWrong() {
        LoginRequest req = new LoginRequest();
        req.setUsername("user");
        req.setPassword("wrong");

        SysUser dbUser = new SysUser();
        dbUser.setPasswordHash("$hash$");

        when(sysUserMapper.selectOne(any(QueryWrapper.class))).thenReturn(dbUser);
        when(passwordEncoder.matches("wrong", "$hash$")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(req));
        assertEquals(ApiCode.PASSWORD_ERROR.getCode(), ex.getCode());
    }
}
