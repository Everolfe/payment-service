package com.github.everolfe.paymentservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreatePaymentDto(
        @NotNull(message = "User ID is required")
        @Positive(message = "User ID cannot be negative")
        Long userId,

        @NotNull(message = "Order ID is required")
        @Positive(message = "Order ID cannot be negative")
        Long orderId,

        @Positive
        BigDecimal amount
) { }
