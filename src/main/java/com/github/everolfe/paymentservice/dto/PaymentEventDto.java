package com.github.everolfe.paymentservice.dto;


import com.github.everolfe.paymentservice.entity.PaymentStatus;

public record PaymentEventDto(
        String paymentId,
        Long orderId,
        Long userId,
        PaymentStatus status
) {}
