package com.github.everolfe.paymentservice.controller;

import com.github.everolfe.paymentservice.dto.CreatePaymentDto;
import com.github.everolfe.paymentservice.dto.GetPaymentDto;
import com.github.everolfe.paymentservice.entity.PaymentStatus;
import com.github.everolfe.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GetPaymentDto> createPayment(
            @Valid @RequestBody CreatePaymentDto createPaymentDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createPayment(createPaymentDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GetPaymentDto> getPayment(@PathVariable String id) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(paymentService.getPaymentById(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<GetPaymentDto>> getAllPayments(Pageable pageable) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(paymentService.getAllPayments(pageable));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<GetPaymentDto>> getPaymentsByUserId(
            @PathVariable Long userId, Pageable pageable) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(paymentService.getPaymentsByUserId(userId,pageable));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Page<GetPaymentDto>> getPaymentsByOrderId(
            @PathVariable Long orderId, Pageable pageable){
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(paymentService.getPaymentsByOrderId(orderId,pageable));
    }

    @GetMapping("/status")
    public ResponseEntity<Page<GetPaymentDto>> getPaymentsByStatus(
            @RequestParam List<PaymentStatus> statuses, Pageable pageable){
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(paymentService.getPaymentsByStatus(statuses,pageable));
    }

    @GetMapping("/total")
    public ResponseEntity<BigDecimal> getPaymentsTotal(
            @RequestParam LocalDateTime from,
            @RequestParam LocalDateTime to) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(paymentService.getAllPaymentTotalResult(from,to));
    }

    @GetMapping("/total/{userId}")
    public ResponseEntity<BigDecimal> getPaymentsTotalByUserId(
            @PathVariable Long userId,
            @RequestParam LocalDateTime from,
            @RequestParam LocalDateTime to) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(paymentService.getAllPaymentTotalResultForCurrentUser(
                        userId, from, to
                ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable String id) {
        paymentService.deletePayment(id);
        return ResponseEntity.noContent().build();
    }


}
