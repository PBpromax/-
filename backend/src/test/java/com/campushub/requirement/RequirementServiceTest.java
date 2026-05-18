package com.campushub.requirement;

import com.campushub.common.exception.BusinessException;
import com.campushub.requirement.dto.CreateRequirementRequest;
import com.campushub.requirement.dto.PageResponse;
import com.campushub.requirement.dto.RequirementDetailResponse;
import com.campushub.requirement.dto.RequirementListItem;
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
class RequirementServiceTest {

    @Autowired
    private RequirementService requirementService;

    @Autowired
    private JdbcClient jdbcClient;

    private static final Long PUBLISHER_ID = 100L;
    private static final Long REQ_TUTORING = 1001L;
    private static final Long REQ_EXPRESS = 1002L;
    private static final Long REQ_COMPLETED = 1003L;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("DELETE FROM biz_evaluation").update();
        jdbcClient.sql("DELETE FROM biz_notification").update();
        jdbcClient.sql("DELETE FROM biz_order").update();
        jdbcClient.sql("DELETE FROM biz_requirement").update();
        jdbcClient.sql("DELETE FROM sys_user").update();

        jdbcClient.sql("""
                INSERT INTO sys_user(user_id, username, nickname, password_hash, phone_encrypted, student_id, campus, credit_score, role)
                VALUES (100, 'pub', '发布者', 'hash', 'enc', 'S001', '南校区', 100, 0)""").update();
        jdbcClient.sql("""
                INSERT INTO sys_user(user_id, username, nickname, password_hash, phone_encrypted, student_id, campus, credit_score, role)
                VALUES (200, 'other', '其他', 'hash', 'enc', 'S002', '北校区', 100, 0)""").update();

        jdbcClient.sql("""
                INSERT INTO biz_requirement(req_id, publisher_id, title, description, budget, type, status)
                VALUES (1001, 100, '数学辅导', '高等数学辅导需求', 50.00, 'TUTORING', 'PENDING')""").update();
        jdbcClient.sql("""
                INSERT INTO biz_requirement(req_id, publisher_id, title, description, budget, type, status)
                VALUES (1002, 200, '代取快递', '帮忙取快递', 10.00, 'EXPRESS', 'PENDING')""").update();
        jdbcClient.sql("""
                INSERT INTO biz_requirement(req_id, publisher_id, title, description, budget, type, status)
                VALUES (1003, 100, '已完成需求', '描述', 20.00, 'TUTORING', 'COMPLETED')""").update();
    }

    // ==================== listRequirements ====================

    @Test
    void listRequirements_ShouldReturnAll() {
        PageResponse<RequirementListItem> page = requirementService.listRequirements(null, null, null, 1, 10);
        assertTrue(page.total() >= 3);
    }

    @Test
    void listRequirements_ShouldFilterByKeyword() {
        PageResponse<RequirementListItem> page = requirementService.listRequirements("快递", null, null, 1, 10);
        assertEquals(1, page.total());
        assertEquals("代取快递", page.list().get(0).title());
    }

    @Test
    void listRequirements_ShouldFilterByType() {
        PageResponse<RequirementListItem> page = requirementService.listRequirements(null, "EXPRESS", null, 1, 10);
        assertEquals(1, page.total());
        assertEquals("EXPRESS", page.list().get(0).type());
    }

    @Test
    void listRequirements_ShouldFilterByStatus() {
        PageResponse<RequirementListItem> page = requirementService.listRequirements(null, null, "COMPLETED", 1, 10);
        assertEquals(1, page.total());
    }

    @Test
    void listRequirements_ShouldHandlePagination() {
        PageResponse<RequirementListItem> page = requirementService.listRequirements(null, null, null, 1, 2);
        assertEquals(2, page.list().size());
        assertEquals(1, page.page());
        assertTrue(page.total() >= 3);
    }

    @Test
    void listRequirements_NoMatch_ShouldReturnEmpty() {
        PageResponse<RequirementListItem> page = requirementService.listRequirements("不存在的关键词xyz", null, null, 1, 10);
        assertEquals(0, page.total());
        assertTrue(page.list().isEmpty());
    }

    // ==================== getRequirementDetail ====================

    @Test
    void getRequirementDetail_ShouldReturnDetail() {
        RequirementDetailResponse detail = requirementService.getRequirementDetail(REQ_TUTORING);

        assertEquals(REQ_TUTORING, detail.reqId());
        assertEquals("数学辅导", detail.title());
        assertEquals("TUTORING", detail.type());
        assertEquals("PENDING", detail.status());
        assertTrue(detail.acceptable(), "PENDING 状态应可接单");
    }

    @Test
    void getRequirementDetail_Completed_ShouldNotAccept() {
        RequirementDetailResponse detail = requirementService.getRequirementDetail(REQ_COMPLETED);

        assertEquals("COMPLETED", detail.status());
        assertFalse(detail.acceptable(), "非 PENDING 状态不可接单");
    }

    @Test
    void getRequirementDetail_NotFound_ShouldThrow() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> requirementService.getRequirementDetail(9999L));
        assertEquals(404, ex.getCode());
    }

    // ==================== createRequirement ====================

    @Test
    void createRequirement_ShouldSucceed() {
        CreateRequirementRequest req = new CreateRequirementRequest(
                "新需求标题", "详细描述", new BigDecimal("100.00"), "TUTORING");

        Long reqId = requirementService.createRequirement(req, PUBLISHER_ID);
        assertNotNull(reqId);

        RequirementDetailResponse detail = requirementService.getRequirementDetail(reqId);
        assertEquals("新需求标题", detail.title());
        assertEquals("PENDING", detail.status());
        assertEquals(PUBLISHER_ID, detail.publisherId());
    }

    @Test
    void createRequirement_InvalidType_ShouldThrow() {
        CreateRequirementRequest req = new CreateRequirementRequest(
                "标题", "描述", new BigDecimal("50.00"), "INVALID_TYPE");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> requirementService.createRequirement(req, PUBLISHER_ID));
        assertEquals(400, ex.getCode());
    }

    // ==================== recommendRequirements ====================

    @Test
    void recommendRequirements_ShouldExcludeOwn() {
        PageResponse<RequirementListItem> page = requirementService.recommendRequirements(PUBLISHER_ID, 1, 10);
        boolean hasOwn = page.list().stream()
                .anyMatch(item -> item.reqId().equals(REQ_TUTORING));
        assertFalse(hasOwn, "推荐不应包含自己的需求");
    }

    @Test
    void recommendRequirements_ShouldOnlyPending() {
        PageResponse<RequirementListItem> page = requirementService.recommendRequirements(200L, 1, 10);
        for (RequirementListItem item : page.list()) {
            assertEquals("PENDING", item.status(), "推荐应只包含 PENDING 需求");
        }
    }
}
