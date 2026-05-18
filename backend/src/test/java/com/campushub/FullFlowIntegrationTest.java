package com.campushub;

import com.campushub.auth.AuthService;
import com.campushub.auth.dto.LoginRequest;
import com.campushub.auth.dto.LoginResponse;
import com.campushub.auth.dto.RegisterRequest;
import com.campushub.common.exception.BusinessException;
import com.campushub.evaluation.dto.EvaluationSubmitReq;
import com.campushub.evaluation.service.EvaluationService;
import com.campushub.order.OrderService;
import com.campushub.order.dto.OrderDetailResponse;
import com.campushub.requirement.RequirementService;
import com.campushub.requirement.dto.CreateRequirementRequest;
import com.campushub.requirement.dto.RequirementDetailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 集成测试：覆盖"注册 → 登录 → 发布需求 → 接单 → 完成 → 评价"完整流程，
 * 以及未登录访问、重复接单、越权操作、自接单 4 类异常流程。
 */
@SpringBootTest
@ActiveProfiles("test")
class FullFlowIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private RequirementService requirementService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private EvaluationService evaluationService;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void cleanDatabase() {
        jdbcClient.sql("DELETE FROM biz_evaluation").update();
        jdbcClient.sql("DELETE FROM biz_notification").update();
        jdbcClient.sql("DELETE FROM biz_order").update();
        jdbcClient.sql("DELETE FROM biz_requirement").update();
        jdbcClient.sql("DELETE FROM sys_user").update();
    }

    // ==================== 主流程：完整正常链路 ====================

    @Test
    void fullHappyPath_RegisterLoginPublishAcceptCompleteEvaluate() {
        // Step 1: Register two users
        RegisterRequest pubReq = new RegisterRequest();
        pubReq.setUsername("publisher_flow");
        pubReq.setPassword("pass123");
        pubReq.setCampus("南校区");
        authService.register(pubReq);

        RegisterRequest recReq = new RegisterRequest();
        recReq.setUsername("receiver_flow");
        recReq.setPassword("pass456");
        recReq.setCampus("北校区");
        authService.register(recReq);

        LoginResponse pubLogin = authService.login(createLoginRequest("publisher_flow", "pass123"));
        LoginResponse recLogin = authService.login(createLoginRequest("receiver_flow", "pass456"));
        Long publisherId = pubLogin.getUserId();
        Long receiverId = recLogin.getUserId();
        assertNotNull(publisherId);
        assertNotNull(receiverId);
        assertNotEquals(publisherId, receiverId);

        // Step 2: Publisher publishes a requirement
        CreateRequirementRequest createReq = new CreateRequirementRequest(
                "集成测试需求-数学辅导", "需要一位数学好的同学帮忙辅导高数。",
                new BigDecimal("80.00"), "TUTORING");
        Long reqId = requirementService.createRequirement(createReq, publisherId);
        assertNotNull(reqId);

        RequirementDetailResponse detail = requirementService.getRequirementDetail(reqId);
        assertEquals("PENDING", detail.status());
        assertEquals(publisherId, detail.publisherId());
        assertTrue(detail.acceptable());

        // Step 3: Receiver accepts the order
        Long orderId = orderService.createOrder(reqId, receiverId);
        assertNotNull(orderId);

        detail = requirementService.getRequirementDetail(reqId);
        assertEquals("ACCEPTED", detail.status());
        assertFalse(detail.acceptable());

        // Step 4: Complete the order via state flow
        OrderDetailResponse submitted = orderService.changeOrderStatus(orderId, receiverId, "SUBMIT");
        assertEquals("TO_CONFIRM", submitted.status());

        OrderDetailResponse completed = orderService.changeOrderStatus(orderId, publisherId, "CONFIRM");
        assertEquals("COMPLETED", completed.status());
        assertNotNull(completed.finishedAt());

        // Step 5: Both parties evaluate each other
        EvaluationSubmitReq pubEval = new EvaluationSubmitReq();
        pubEval.setStar(5);
        pubEval.setContent("非常靠谱！");
        evaluationService.submitEvaluation(orderId, publisherId, pubEval);

        EvaluationSubmitReq recEval = new EvaluationSubmitReq();
        recEval.setStar(4);
        recEval.setContent("需求描述清晰！");
        evaluationService.submitEvaluation(orderId, receiverId, recEval);

        Long evalCount = jdbcClient.sql("SELECT COUNT(*) FROM biz_evaluation WHERE order_id = :oid")
                .param("oid", orderId).query(Long.class).single();
        assertEquals(2L, evalCount, "应有两方各一条评价");
    }

    // ==================== 异常流程 1: 未登录 / 资源不存在 ====================

    @Test
    void abnormal_Unauthenticated_ShouldReturn404() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> requirementService.getRequirementDetail(999999L));
        assertEquals(404, ex.getCode());
    }

    // ==================== 异常流程 2: 重复接单 ====================

    @Test
    void abnormal_DuplicateOrder_ShouldReturn4001() {
        // Setup: register two users and publish a requirement
        registerUser("dup_pub", "pass1");
        registerUser("dup_rec", "pass2");
        Long pubId = authService.login(createLoginRequest("dup_pub", "pass1")).getUserId();
        Long recId = authService.login(createLoginRequest("dup_rec", "pass2")).getUserId();

        CreateRequirementRequest req = new CreateRequirementRequest(
                "防重复需求", "测试重复接单保护", new BigDecimal("30.00"), "EXPRESS");
        Long newReqId = requirementService.createRequirement(req, pubId);

        // First accept succeeds
        orderService.createOrder(newReqId, recId);

        // Second accept fails
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.createOrder(newReqId, recId));
        assertEquals(4001, ex.getCode());
    }

    // ==================== 异常流程 3: 越权访问 ====================

    @Test
    void abnormal_Unauthorized_ThirdPartyAccess_ShouldBeRejected() {
        registerUser("auth_pub", "pass1");
        registerUser("auth_rec", "pass2");
        registerUser("auth_third", "pass3");
        Long pubId = authService.login(createLoginRequest("auth_pub", "pass1")).getUserId();
        Long recId = authService.login(createLoginRequest("auth_rec", "pass2")).getUserId();
        Long thirdId = authService.login(createLoginRequest("auth_third", "pass3")).getUserId();

        CreateRequirementRequest req = new CreateRequirementRequest(
                "越权测试需求", "测试越权保护", new BigDecimal("20.00"), "TUTORING");
        Long newReqId = requirementService.createRequirement(req, pubId);
        Long newOrderId = orderService.createOrder(newReqId, recId);

        // Third party cannot view order detail
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.getOrderDetail(newOrderId, thirdId));
        assertEquals(403, ex.getCode());

        // Third party cannot submit evaluation on incomplete order → 4003
        EvaluationSubmitReq eval = new EvaluationSubmitReq();
        eval.setStar(5);
        BusinessException evalEx1 = assertThrows(BusinessException.class,
                () -> evaluationService.submitEvaluation(newOrderId, thirdId, eval));
        assertEquals(4003, evalEx1.getCode(), "非完成订单应先返回4003");

        // Complete the order first
        orderService.changeOrderStatus(newOrderId, recId, "SUBMIT");
        orderService.changeOrderStatus(newOrderId, pubId, "CONFIRM");

        // Third party cannot submit evaluation on completed order → 4004
        BusinessException evalEx2 = assertThrows(BusinessException.class,
                () -> evaluationService.submitEvaluation(newOrderId, thirdId, eval));
        assertEquals(4004, evalEx2.getCode(), "非参与者评价已完成订单应返回4004");
    }

    // ==================== 异常流程 4: 发布者自接单 ====================

    @Test
    void abnormal_SelfOrder_ShouldReturn4002() {
        registerUser("self_pub", "pass1");
        Long pubId = authService.login(createLoginRequest("self_pub", "pass1")).getUserId();

        CreateRequirementRequest req = new CreateRequirementRequest(
                "自接单测试", "测试发布者接自己的需求", new BigDecimal("10.00"), "EXPRESS");
        Long myReqId = requirementService.createRequirement(req, pubId);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.createOrder(myReqId, pubId));
        assertEquals(4002, ex.getCode());
    }

    // ==================== 异常流程 5: 非法状态跳转 ====================

    @Test
    void abnormal_InvalidStateTransition_ShouldBeRejected() {
        registerUser("state_pub", "pass1");
        registerUser("state_rec", "pass2");
        Long pubId = authService.login(createLoginRequest("state_pub", "pass1")).getUserId();
        Long recId = authService.login(createLoginRequest("state_rec", "pass2")).getUserId();

        CreateRequirementRequest req = new CreateRequirementRequest(
                "状态跳转测试", "测试非法状态转换", new BigDecimal("15.00"), "TUTORING");
        Long rId = requirementService.createRequirement(req, pubId);
        Long oId = orderService.createOrder(rId, recId);

        // Receiver cannot CONFIRM from IN_PROGRESS (only publisher can, and only from TO_CONFIRM)
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.changeOrderStatus(oId, pubId, "CONFIRM"));
        assertEquals(400, ex.getCode());
    }

    // ==================== helpers ====================

    private void registerUser(String username, String password) {
        RegisterRequest req = new RegisterRequest();
        req.setUsername(username);
        req.setPassword(password);
        authService.register(req);
    }

    private static LoginRequest createLoginRequest(String username, String password) {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);
        return req;
    }
}
