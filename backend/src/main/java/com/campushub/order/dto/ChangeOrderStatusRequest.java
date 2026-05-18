package com.campushub.order.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangeOrderStatusRequest(
        @NotBlank(message = "订单事件不能为空")
        String event
) {
}
