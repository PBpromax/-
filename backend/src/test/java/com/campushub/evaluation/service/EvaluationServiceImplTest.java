package com.campushub.evaluation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campushub.common.exception.BusinessException;
import com.campushub.entity.BizOrder;
import com.campushub.entity.BizRequirement;
import com.campushub.entity.OrderStatus;
import com.campushub.entity.SysUser;
import com.campushub.evaluation.dto.EvaluationSubmitReq;
import com.campushub.evaluation.service.impl.EvaluationServiceImpl;
import com.campushub.mapper.BizEvaluationMapper;
import com.campushub.mapper.BizOrderMapper;
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
class EvaluationServiceImplTest {

    @Mock
    private BizOrderMapper bizOrderMapper;
    @Mock
    private BizRequirementMapper bizRequirementMapper;
    @Mock
    private BizEvaluationMapper bizEvaluationMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private EvaluationServiceImpl evaluationService;

    private BizOrder mockOrder;
    private BizRequirement mockReq;
    private SysUser mockTargetUser;
    private EvaluationSubmitReq mockReqDto;

    @BeforeEach
    void setUp() {
        mockOrder = new BizOrder();
        mockOrder.setOrderId(1L);
        mockOrder.setReqId(10L);
        mockOrder.setStatus(OrderStatus.COMPLETED.name());
        mockOrder.setReceiverId(200L);

        mockReq = new BizRequirement();
        mockReq.setReqId(10L);
        mockReq.setPublisherId(100L);

        mockTargetUser = new SysUser();
        mockTargetUser.setUserId(200L);
        mockTargetUser.setCreditScore(90);

        mockReqDto = new EvaluationSubmitReq();
    }

    @Test
    void testSubmitEvaluation_5Stars_ShouldAdd2Points() {
        mockReqDto.setStar(5);
        mockReqDto.setContent("太棒了!");

        when(bizOrderMapper.selectById(1L)).thenReturn(mockOrder);
        when(bizRequirementMapper.selectById(10L)).thenReturn(mockReq);
        when(bizEvaluationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(sysUserMapper.selectById(200L)).thenReturn(mockTargetUser);

        evaluationService.submitEvaluation(1L, 100L, mockReqDto);

        verify(sysUserMapper).updateById(ArgumentMatchers.<SysUser>argThat(user ->
                user.getCreditScore() == 92));
        verify(notificationService).createNotification(
                eq(200L), anyString(), anyString(), eq("EVALUATION_SUBMITTED"));
    }

    @Test
    void testSubmitEvaluation_5Stars_ShouldNotExceed100() {
        mockTargetUser.setCreditScore(100);
        mockReqDto.setStar(5);

        when(bizOrderMapper.selectById(1L)).thenReturn(mockOrder);
        when(bizRequirementMapper.selectById(10L)).thenReturn(mockReq);
        when(bizEvaluationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(sysUserMapper.selectById(200L)).thenReturn(mockTargetUser);

        evaluationService.submitEvaluation(1L, 100L, mockReqDto);

        verify(sysUserMapper).updateById(ArgumentMatchers.<SysUser>argThat(user ->
                user.getCreditScore() == 100));
    }

    @Test
    void testSubmitEvaluation_1Star_ShouldDeduct2PointsAndNotBelowZero() {
        mockTargetUser.setCreditScore(1);
        mockReqDto.setStar(1);

        when(bizOrderMapper.selectById(1L)).thenReturn(mockOrder);
        when(bizRequirementMapper.selectById(10L)).thenReturn(mockReq);
        when(bizEvaluationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(sysUserMapper.selectById(200L)).thenReturn(mockTargetUser);

        evaluationService.submitEvaluation(1L, 100L, mockReqDto);

        verify(sysUserMapper).updateById(ArgumentMatchers.<SysUser>argThat(user ->
                user.getCreditScore() == 0));
        verify(notificationService).createNotification(
                eq(200L), anyString(), anyString(), eq("EVALUATION_SUBMITTED"));
    }

    @Test
    void testSubmitEvaluation_OrderNotCompleted_ShouldThrowException() {
        mockOrder.setStatus(OrderStatus.IN_PROGRESS.name());
        when(bizOrderMapper.selectById(1L)).thenReturn(mockOrder);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                evaluationService.submitEvaluation(1L, 100L, mockReqDto));

        assertEquals(4003, exception.getCode());
    }

    @Test
    void testSubmitEvaluation_4Stars_ShouldAdd1Point() {
        mockTargetUser.setCreditScore(90);
        mockReqDto.setStar(4);

        when(bizOrderMapper.selectById(1L)).thenReturn(mockOrder);
        when(bizRequirementMapper.selectById(10L)).thenReturn(mockReq);
        when(bizEvaluationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(sysUserMapper.selectById(200L)).thenReturn(mockTargetUser);

        evaluationService.submitEvaluation(1L, 100L, mockReqDto);

        verify(sysUserMapper).updateById(ArgumentMatchers.<SysUser>argThat(user ->
                user.getCreditScore() == 91));
    }

    @Test
    void testSubmitEvaluation_3Stars_ShouldNotChangeScore() {
        mockTargetUser.setCreditScore(90);
        mockReqDto.setStar(3);

        when(bizOrderMapper.selectById(1L)).thenReturn(mockOrder);
        when(bizRequirementMapper.selectById(10L)).thenReturn(mockReq);
        when(bizEvaluationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(sysUserMapper.selectById(200L)).thenReturn(mockTargetUser);

        evaluationService.submitEvaluation(1L, 100L, mockReqDto);

        verify(sysUserMapper, never()).updateById(ArgumentMatchers.<SysUser>any());
        verify(notificationService).createNotification(
                eq(200L), anyString(), anyString(), eq("EVALUATION_SUBMITTED"));
    }

    @Test
    void testSubmitEvaluation_2Stars_ShouldDeduct1Point() {
        mockTargetUser.setCreditScore(90);
        mockReqDto.setStar(2);

        when(bizOrderMapper.selectById(1L)).thenReturn(mockOrder);
        when(bizRequirementMapper.selectById(10L)).thenReturn(mockReq);
        when(bizEvaluationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(sysUserMapper.selectById(200L)).thenReturn(mockTargetUser);

        evaluationService.submitEvaluation(1L, 100L, mockReqDto);

        verify(sysUserMapper).updateById(ArgumentMatchers.<SysUser>argThat(user ->
                user.getCreditScore() == 89));
    }

    @Test
    void testSubmitEvaluation_NotParticipant_ShouldThrowException() {
        mockReqDto.setStar(5);
        when(bizOrderMapper.selectById(1L)).thenReturn(mockOrder);
        when(bizRequirementMapper.selectById(10L)).thenReturn(mockReq);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                evaluationService.submitEvaluation(1L, 999L, mockReqDto));
        assertEquals(4004, ex.getCode());
    }

    @Test
    void testSubmitEvaluation_DuplicateEvaluation_ShouldThrowException() {
        mockReqDto.setStar(5);
        when(bizOrderMapper.selectById(1L)).thenReturn(mockOrder);
        when(bizRequirementMapper.selectById(10L)).thenReturn(mockReq);
        when(bizEvaluationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                evaluationService.submitEvaluation(1L, 100L, mockReqDto));
        assertEquals(400, ex.getCode());
    }

    @Test
    void testSubmitEvaluation_OrderNotFound_ShouldThrowException() {
        when(bizOrderMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                evaluationService.submitEvaluation(999L, 100L, mockReqDto));
        assertEquals(404, ex.getCode());
        assertEquals("查询的订单不存在", ex.getMessage());
    }

    @Test
    void testSubmitEvaluation_RequirementNotFound_ShouldThrowException() {
        when(bizOrderMapper.selectById(1L)).thenReturn(mockOrder);
        when(bizRequirementMapper.selectById(10L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                evaluationService.submitEvaluation(1L, 100L, mockReqDto));
        assertEquals(404, ex.getCode());
    }
}
