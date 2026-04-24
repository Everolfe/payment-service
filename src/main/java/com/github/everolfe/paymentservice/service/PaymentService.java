package com.github.everolfe.paymentservice.service;

import com.github.everolfe.paymentservice.dto.CreatePaymentDto;
import com.github.everolfe.paymentservice.dto.GetPaymentDto;
import com.github.everolfe.paymentservice.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {

    GetPaymentDto createPayment(CreatePaymentDto createPaymentDto);

    GetPaymentDto getPaymentById(String id);

    Page<GetPaymentDto> getAllPayments(Pageable pageable);

    Page<GetPaymentDto> getPaymentsByUserId(Long userId, Pageable pageable);

    Page<GetPaymentDto> getPaymentsByOrderId(Long orderId, Pageable pageable);

    Page<GetPaymentDto> getPaymentsByStatus(Collection<PaymentStatus> statuses, Pageable pageable);

    BigDecimal getAllPaymentTotalResult(LocalDateTime from, LocalDateTime to);

    BigDecimal getAllPaymentTotalResultForCurrentUser(
            Long userId, LocalDateTime from, LocalDateTime to);

    void deletePayment(String id);
}
