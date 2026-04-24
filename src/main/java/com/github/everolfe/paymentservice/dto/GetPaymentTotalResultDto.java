package com.github.everolfe.paymentservice.dto;

import java.math.BigDecimal;

public record GetPaymentTotalResultDto(
        BigDecimal amount
) {
}
