package com.github.everolfe.paymentservice.dto;

import com.github.everolfe.paymentservice.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GetPaymentDto(
        String id,
        Long userId,
        Long orderId,
        PaymentStatus status,
        LocalDateTime timestamp,
        BigDecimal amount
) {
}
