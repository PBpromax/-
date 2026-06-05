package com.campushub.profile;

import com.campushub.common.exception.BusinessException;
import com.campushub.profile.dto.ProfileResponse;
import com.campushub.profile.dto.ProfileUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProfileServiceTest {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private JdbcClient jdbcClient;

    private static final Long USER_ID = 100L;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("DELETE FROM biz_evaluation").update();
        jdbcClient.sql("DELETE FROM biz_notification").update();
        jdbcClient.sql("DELETE FROM biz_order").update();
        jdbcClient.sql("DELETE FROM biz_requirement").update();
        jdbcClient.sql("DELETE FROM sys_user").update();

        jdbcClient.sql("""
                INSERT INTO sys_user(user_id, username, nickname, password_hash, phone_encrypted,
                                     student_id, contact_qq, contact_wechat, contact_phone, campus, college, major, grade, bio, contact_visible, credit_score, role)
                VALUES (100, 'testuser', '测试昵称', 'hash', 'enc', 'S001', '10000', 'wechat_a', '13800000000', '南校区', '计算机学院', '软件工程', '大三',
                        '个人简介', 1, 100, 0)""").update();
    }

    @Test
    void getProfile_ShouldReturnProfile() {
        ProfileResponse profile = profileService.getProfile(USER_ID);

        assertEquals(USER_ID, profile.userId());
        assertEquals("testuser", profile.username());
        assertEquals("测试昵称", profile.nickname());
        assertEquals("S001", profile.studentId());
        assertEquals("10000", profile.qq());
        assertEquals("wechat_a", profile.wechat());
        assertEquals("13800000000", profile.phone());
        assertEquals("南校区", profile.campus());
        assertEquals("计算机学院", profile.college());
        assertEquals("软件工程", profile.major());
        assertEquals("大三", profile.grade());
        assertEquals("个人简介", profile.bio());
        assertTrue(profile.contactVisible());
        assertEquals(100, profile.creditScore());
    }

    @Test
    void getProfile_ShouldCapCreditScoreAt100() {
        jdbcClient.sql("UPDATE sys_user SET credit_score = 120 WHERE user_id = :userId")
                .param("userId", USER_ID)
                .update();

        ProfileResponse profile = profileService.getProfile(USER_ID);

        assertEquals(100, profile.creditScore());
    }

    @Test
    void getProfile_NotFound_ShouldThrow() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> profileService.getProfile(999L));
        assertEquals(404, ex.getCode());
    }

    @Test
    void updateProfile_ShouldUpdateFields() {
        ProfileUpdateRequest req = new ProfileUpdateRequest(
                "新昵称", "S002", "20000", "wechat_b", "13900000000", "北校区", "数学学院", "应用数学", "大二", "新简介", false);

        ProfileResponse updated = profileService.updateProfile(USER_ID, req);

        assertEquals("新昵称", updated.nickname());
        assertEquals("S002", updated.studentId());
        assertEquals("20000", updated.qq());
        assertEquals("wechat_b", updated.wechat());
        assertEquals("13900000000", updated.phone());
        assertEquals("北校区", updated.campus());
        assertEquals("数学学院", updated.college());
        assertEquals("应用数学", updated.major());
        assertEquals("大二", updated.grade());
        assertEquals("新简介", updated.bio());
        assertFalse(updated.contactVisible());

        // Verify persistence
        ProfileResponse persisted = profileService.getProfile(USER_ID);
        assertEquals("新昵称", persisted.nickname());
    }

    @Test
    void updateProfile_NotFound_ShouldThrow() {
        ProfileUpdateRequest req = new ProfileUpdateRequest(
                "昵称", "S002", "20000", "wechat_b", "13900000000", "校区", "学院", "专业", "年级", "简介", true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> profileService.updateProfile(999L, req));
        assertEquals(404, ex.getCode());
    }

    @Test
    void updateProfile_PartialFields_ShouldNotAffectOthers() {
        ProfileUpdateRequest req = new ProfileUpdateRequest(
                "新昵称", "S001", null, null, null, null, null, null, null, null, null);

        ProfileResponse updated = profileService.updateProfile(USER_ID, req);
        assertEquals("新昵称", updated.nickname());
        assertEquals("testuser", updated.username(), "username 不应改变");
    }
}
