package com.campushub.requirement.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RequirementDetailResponse(
        Long reqId,
        Long publisherId,
        String publisherName,
        String publisherQq,
        String publisherWechat,
        String publisherPhone,
        String title,
        String description,
        BigDecimal budget,
        String type,
        String status,
        LocalDateTime createdAt,
        boolean acceptable
) {
}
