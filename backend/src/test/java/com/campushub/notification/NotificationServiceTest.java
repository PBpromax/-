package com.campushub.notification;

import com.campushub.common.exception.BusinessException;
import com.campushub.notification.dto.NotificationItem;
import com.campushub.notification.dto.UnreadCountResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private JdbcClient jdbcClient;

    private static final Long USER_A = 100L;
    private static final Long USER_B = 200L;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("DELETE FROM biz_notification").update();
        jdbcClient.sql("DELETE FROM biz_evaluation").update();
        jdbcClient.sql("DELETE FROM biz_order").update();
        jdbcClient.sql("DELETE FROM biz_requirement").update();
        jdbcClient.sql("DELETE FROM sys_user").update();

        jdbcClient.sql("""
                INSERT INTO sys_user(user_id, username, password_hash, phone_encrypted, student_id, credit_score, role)
                VALUES (100, 'userA', 'hash', 'enc', 'S001', 100, 0)""").update();
        jdbcClient.sql("""
                INSERT INTO sys_user(user_id, username, password_hash, phone_encrypted, student_id, credit_score, role)
                VALUES (200, 'userB', 'hash', 'enc', 'S002', 100, 0)""").update();
    }

    // ==================== createNotification ====================

    @Test
    void createNotification_ShouldInsertRecord() {
        notificationService.createNotification(USER_A, "测试标题", "测试内容", "TEST_EVENT");

        List<NotificationItem> list = notificationService.listNotifications(USER_A, false);
        assertEquals(1, list.size());
        assertEquals("测试标题", list.get(0).title());
        assertEquals("TEST_EVENT", list.get(0).eventType());
        assertFalse(list.get(0).read());
    }

    @Test
    void createNotification_MultipleForSameUser_ShouldAllPersist() {
        notificationService.createNotification(USER_A, "标题1", "内容1", "EVENT_A");
        notificationService.createNotification(USER_A, "标题2", "内容2", "EVENT_B");
        notificationService.createNotification(USER_A, "标题3", "内容3", "EVENT_C");

        List<NotificationItem> list = notificationService.listNotifications(USER_A, false);
        assertEquals(3, list.size());
    }

    // ==================== listNotifications ====================

    @Test
    void listNotifications_ShouldReturnAllForUser() {
        notificationService.createNotification(USER_A, "A的通知", "", "EVENT");
        notificationService.createNotification(USER_B, "B的通知", "", "EVENT");

        List<NotificationItem> listA = notificationService.listNotifications(USER_A, false);
        assertEquals(1, listA.size());
        assertEquals("A的通知", listA.get(0).title());

        List<NotificationItem> listB = notificationService.listNotifications(USER_B, false);
        assertEquals(1, listB.size());
        assertEquals("B的通知", listB.get(0).title());
    }

    @Test
    void listNotifications_UnreadOnly_ShouldFilter() {
        notificationService.createNotification(USER_A, "未读", "", "EVENT");
        notificationService.createNotification(USER_A, "未读2", "", "EVENT");

        var notifications = notificationService.listNotifications(USER_A, false);
        notificationService.markAsRead(USER_A, notifications.get(0).notificationId());

        List<NotificationItem> unread = notificationService.listNotifications(USER_A, true);
        assertEquals(1, unread.size());
    }

    @Test
    void listNotifications_EmptyUser_ShouldReturnEmptyList() {
        List<NotificationItem> list = notificationService.listNotifications(999L, false);
        assertTrue(list.isEmpty());
    }

    // ==================== getUnreadCount ====================

    @Test
    void getUnreadCount_ShouldReturnCorrectCount() {
        notificationService.createNotification(USER_A, "1", "", "E");
        notificationService.createNotification(USER_A, "2", "", "E");
        notificationService.createNotification(USER_A, "3", "", "E");

        UnreadCountResponse resp = notificationService.getUnreadCount(USER_A);
        assertEquals(3, resp.unreadCount());
    }

    @Test
    void getUnreadCount_NoNotifications_ShouldReturnZero() {
        UnreadCountResponse resp = notificationService.getUnreadCount(USER_A);
        assertEquals(0, resp.unreadCount());
    }

    // ==================== markAsRead ====================

    @Test
    void markAsRead_ShouldUpdateStatus() {
        notificationService.createNotification(USER_A, "标题", "内容", "EVENT");
        var notifications = notificationService.listNotifications(USER_A, false);
        Long notifId = notifications.get(0).notificationId();

        notificationService.markAsRead(USER_A, notifId);

        List<NotificationItem> after = notificationService.listNotifications(USER_A, false);
        assertTrue(after.get(0).read());
    }

    @Test
    void markAsRead_WrongUser_ShouldThrow() {
        notificationService.createNotification(USER_A, "标题", "内容", "EVENT");
        var notifications = notificationService.listNotifications(USER_A, false);
        Long notifId = notifications.get(0).notificationId();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> notificationService.markAsRead(USER_B, notifId));
        assertEquals(404, ex.getCode());
    }

    @Test
    void markAsRead_NotFound_ShouldThrow() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> notificationService.markAsRead(USER_A, 99999L));
        assertEquals(404, ex.getCode());
    }

    // ==================== markAllAsRead ====================

    @Test
    void markAllAsRead_ShouldUpdateAllUnread() {
        notificationService.createNotification(USER_A, "1", "", "E");
        notificationService.createNotification(USER_A, "2", "", "E");
        notificationService.createNotification(USER_A, "3", "", "E");

        int updated = notificationService.markAllAsRead(USER_A);
        assertEquals(3, updated);

        UnreadCountResponse resp = notificationService.getUnreadCount(USER_A);
        assertEquals(0, resp.unreadCount());
    }

    @Test
    void markAllAsRead_NoUnread_ShouldReturnZero() {
        int updated = notificationService.markAllAsRead(USER_A);
        assertEquals(0, updated);
    }

    @Test
    void markAllAsRead_OnlyAffectsCurrentUser() {
        notificationService.createNotification(USER_A, "A", "", "E");
        notificationService.createNotification(USER_B, "B", "", "E");

        notificationService.markAllAsRead(USER_A);

        assertEquals(0, notificationService.getUnreadCount(USER_A).unreadCount());
        assertEquals(1, notificationService.getUnreadCount(USER_B).unreadCount());
    }

    @Test
    void deleteReadNotifications_ShouldDeleteOnlyReadForCurrentUser() {
        notificationService.createNotification(USER_A, "A未读", "", "E");
        notificationService.createNotification(USER_A, "A已读", "", "E");
        notificationService.createNotification(USER_B, "B已读", "", "E");

        Long readA = notificationService.listNotifications(USER_A, false).get(0).notificationId();
        Long readB = notificationService.listNotifications(USER_B, false).get(0).notificationId();
        notificationService.markAsRead(USER_A, readA);
        notificationService.markAsRead(USER_B, readB);

        int deleted = notificationService.deleteReadNotifications(USER_A);

        assertEquals(1, deleted);
        assertEquals(1, notificationService.listNotifications(USER_A, false).size());
        assertEquals(1, notificationService.listNotifications(USER_B, false).size());
    }
}
