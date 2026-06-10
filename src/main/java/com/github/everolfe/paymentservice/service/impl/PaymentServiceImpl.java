package com.github.everolfe.paymentservice.service.impl;

import com.github.everolfe.paymentservice.client.RandomNumberClient;
import com.github.everolfe.paymentservice.dao.PaymentRepository;
import com.github.everolfe.paymentservice.dto.CreatePaymentDto;
import com.github.everolfe.paymentservice.dto.GetPaymentDto;
import com.github.everolfe.paymentservice.dto.GetPaymentTotalResultDto;
import com.github.everolfe.paymentservice.dto.PaymentEventDto;
import com.github.everolfe.paymentservice.entity.Payment;
import com.github.everolfe.paymentservice.entity.PaymentStatus;
import com.github.everolfe.paymentservice.mapper.event.PaymentEventMapper;
import com.github.everolfe.paymentservice.mapper.payment.CreatePaymentMapper;
import com.github.everolfe.paymentservice.mapper.payment.GetPaymentMapper;
import com.github.everolfe.paymentservice.security.SecurityHelper;
import com.github.everolfe.paymentservice.service.PaymentService;
import com.github.everolfe.paymentservice.service.event.PaymentProducer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    private final CreatePaymentMapper createPaymentMapper;

    private final GetPaymentMapper getPaymentMapper;

    private final SecurityHelper securityHelper;

    private final RandomNumberClient randomNumberClient;

    private final PaymentProducer paymentProducer;

    private final PaymentEventMapper paymentEventMapper;

    @Override
    @Transactional
    public GetPaymentDto createPayment(CreatePaymentDto createPaymentDto) {

        Payment payment = createPaymentMapper.toEntity(createPaymentDto);

        Integer randomNumber = randomNumberClient.getRandomNumber();
        if (randomNumber == 0) {
            payment.setStatus(PaymentStatus.PENDING);
        } else if (randomNumber % 2 == 0) {
            payment.setStatus(PaymentStatus.SUCCESS);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }
        Payment savedPayment = paymentRepository.save(payment);
        PaymentEventDto paymentEventDto = paymentEventMapper.toDto(savedPayment);
        paymentProducer.sendPaymentCreatedEvent(paymentEventDto);
        return getPaymentMapper.toDto(savedPayment);
    }


    @Override
    @Transactional(readOnly = true)
    public GetPaymentDto getPaymentById(String id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: ", id));
        checkAccess(payment.getUserId());
        return getPaymentMapper.toDto(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GetPaymentDto> getAllPayments(Pageable pageable){
        Page<Payment> payments = paymentRepository.findAll(pageable);
        return payments.map(getPaymentMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GetPaymentDto> getPaymentsByUserId(Long userId, Pageable pageable){
        checkAccess(userId);
        Page<Payment> payments = paymentRepository.findByUserId(userId, pageable);
        return payments.map(getPaymentMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GetPaymentDto> getPaymentsByOrderId(Long orderId, Pageable pageable){
        Page<Payment> payments = paymentRepository.findByOrderId(orderId, pageable);
        return payments.map(getPaymentMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GetPaymentDto> getPaymentsByStatus(Collection<PaymentStatus> status,
                                                   Pageable pageable){
        Page<Payment> payments = paymentRepository.findAllByStatusIn(status,pageable);
        return payments.map(getPaymentMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getAllPaymentTotalResult(
            LocalDateTime from, LocalDateTime to){
        GetPaymentTotalResultDto resultDto = paymentRepository.sumPaymentInPeriod(from, to);
        return (resultDto != null && resultDto.amount()!=null) ? resultDto.amount() : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getAllPaymentTotalResultForCurrentUser(
            Long userId, LocalDateTime from, LocalDateTime to){
        checkAccess(userId);
        GetPaymentTotalResultDto resultDto = paymentRepository.sumPaymentByUserInPeriod(userId,from, to);
        return (resultDto != null && resultDto.amount()!=null) ? resultDto.amount() : BigDecimal.ZERO;
    }

    @Override
    @Transactional
    public void deletePayment(String id) {
        paymentRepository.deleteById(id);
    }

    private void checkAccess(Long userId){
        Long currentUserId = securityHelper.getCurrentUserId();
        if(!securityHelper.isAdmin() && !currentUserId.equals(userId)){
            throw new AccessDeniedException("You do not have permission to access this resource");
        }
    }
}
