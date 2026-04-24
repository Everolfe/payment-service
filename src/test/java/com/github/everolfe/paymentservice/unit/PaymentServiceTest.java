package com.github.everolfe.paymentservice.unit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.everolfe.paymentservice.client.RandomNumberClient;
import com.github.everolfe.paymentservice.dao.PaymentRepository;
import com.github.everolfe.paymentservice.dto.CreatePaymentDto;
import com.github.everolfe.paymentservice.dto.GetPaymentDto;
import com.github.everolfe.paymentservice.dto.PaymentEventDto;
import com.github.everolfe.paymentservice.dto.GetPaymentTotalResultDto;
import com.github.everolfe.paymentservice.entity.Payment;
import com.github.everolfe.paymentservice.entity.PaymentStatus;
import com.github.everolfe.paymentservice.mapper.payment.CreatePaymentMapper;
import com.github.everolfe.paymentservice.mapper.payment.GetPaymentMapper;
import com.github.everolfe.paymentservice.mapper.event.PaymentEventMapper;
import com.github.everolfe.paymentservice.security.SecurityHelper;
import com.github.everolfe.paymentservice.service.event.PaymentProducer;
import com.github.everolfe.paymentservice.service.impl.PaymentServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private RandomNumberClient randomNumberClient;

    @Mock
    private PaymentRepository paymentRepository;

    @Spy
    private GetPaymentMapper getPaymentMapper;

    @Spy
    private CreatePaymentMapper createPaymentMapper;

    @Spy
    private PaymentEventMapper getPaymentEventMapper;

    @Mock
    private PaymentProducer paymentProducer;

    @Mock
    private SecurityHelper securityHelper;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void test_createPayment_evenExternalApiResponse() {

        Integer EVEN_EXTERNAL_API_RESPONSE = 2;
        PaymentStatus EXPECTED_PAYMENT_STATUS = PaymentStatus.SUCCESS;

        CreatePaymentDto createPaymentDto = new CreatePaymentDto(
                1L, 1L, new BigDecimal(1000));

        Payment payment = new Payment(
                null,
                createPaymentDto.userId(), createPaymentDto.orderId(),
                LocalDateTime.now(), createPaymentDto.amount(),null);

        when(createPaymentMapper.toEntity(createPaymentDto)).thenReturn(payment);

        when(randomNumberClient.getRandomNumber()).thenReturn(EVEN_EXTERNAL_API_RESPONSE);

        when(paymentRepository.save(any(Payment.class))).thenAnswer(
                invocation -> {
                    Payment paymentLocal = invocation.getArgument(0);
                    paymentLocal.setId("694a6081723088150e7cf74c");
                    return paymentLocal;
                }
        );

        when(getPaymentEventMapper.toDto(any(Payment.class))).thenAnswer(
                invocation -> {
                    Payment paymentLocal = invocation.getArgument(0);
                    return new PaymentEventDto(
                            paymentLocal.getId(),
                            paymentLocal.getOrderId(),
                            paymentLocal.getUserId(),
                            paymentLocal.getStatus()
                    );
                }
        );
        doNothing().when(paymentProducer).sendPaymentCreatedEvent(any(PaymentEventDto.class));

        when(getPaymentMapper.toDto(any(Payment.class))).thenAnswer(
                invocation -> {
                    Payment paymentLocal = invocation.getArgument(0);
                    return new GetPaymentDto(
                            paymentLocal.getId(),
                            paymentLocal.getOrderId(),
                            paymentLocal.getUserId(),
                            paymentLocal.getStatus(),
                            paymentLocal.getTimestamp(),
                            paymentLocal.getAmount()
                    );
                }
        );

        GetPaymentDto result = paymentService.createPayment(createPaymentDto);

        assertThat(result.status()).isEqualTo(EXPECTED_PAYMENT_STATUS);

        verify(paymentProducer, times(1)).sendPaymentCreatedEvent(any(PaymentEventDto.class));
    }

    @Test
    void test_createPayment_oddExternalApiResponse() {

        Integer ODD_EXTERNAL_API_RESPONSE = 1;
        PaymentStatus EXPECTED_PAYMENT_STATUS = PaymentStatus.FAILED;

        CreatePaymentDto createPaymentDto = new CreatePaymentDto(
                1L, 1L, new BigDecimal(1000));

        Payment payment = new Payment(
                null,
                createPaymentDto.userId(), createPaymentDto.orderId(),
                LocalDateTime.now(), createPaymentDto.amount(), null);

        Payment savedPayment = new Payment(
                "694a6081723088150e7cf74c",
                createPaymentDto.userId(), createPaymentDto.orderId(),
                LocalDateTime.now(), createPaymentDto.amount(), PaymentStatus.FAILED);

        PaymentEventDto paymentEventDto = new PaymentEventDto(
                savedPayment.getId(),
                savedPayment.getUserId(),
                savedPayment.getOrderId(),
                savedPayment.getStatus()
        );

        GetPaymentDto expectedResult = new GetPaymentDto(
                savedPayment.getId(),
                savedPayment.getOrderId(),
                savedPayment.getUserId(),
                savedPayment.getStatus(),
                savedPayment.getTimestamp(),
                savedPayment.getAmount()
        );

        when(createPaymentMapper.toEntity(createPaymentDto)).thenReturn(payment);
        when(randomNumberClient.getRandomNumber()).thenReturn(ODD_EXTERNAL_API_RESPONSE);

        when(getPaymentEventMapper.toDto(any(Payment.class))).thenReturn(paymentEventDto);

        when(getPaymentMapper.toDto(any(Payment.class))).thenReturn(expectedResult);

        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        doNothing().when(paymentProducer).sendPaymentCreatedEvent(any(PaymentEventDto.class));

        GetPaymentDto result = paymentService.createPayment(createPaymentDto);

        assertThat(result.status()).isEqualTo(EXPECTED_PAYMENT_STATUS);

        verify(getPaymentEventMapper, times(1)).toDto(any(Payment.class));
        verify(getPaymentMapper, times(1)).toDto(any(Payment.class));
        verify(paymentProducer, times(1)).sendPaymentCreatedEvent(any(PaymentEventDto.class));
    }

    @Test
    void test_createPayment_noExternalApiResponse() {

        Integer NO_EXTERNAL_API_RESPONSE = 0;
        PaymentStatus EXPECTED_PAYMENT_STATUS = PaymentStatus.PENDING;

        CreatePaymentDto createPaymentDto = new CreatePaymentDto(
                1L, 1L, new BigDecimal(1000));

        Payment payment = new Payment(
                null,
                createPaymentDto.userId(), createPaymentDto.orderId(),
                LocalDateTime.now(), createPaymentDto.amount(), null);

        when(createPaymentMapper.toEntity(createPaymentDto)).thenReturn(payment);
        when(randomNumberClient.getRandomNumber()).thenReturn(NO_EXTERNAL_API_RESPONSE);

        // Create expected saved payment
        Payment savedPayment = new Payment(
                "694a6081723088150e7cf74c",
                createPaymentDto.userId(), createPaymentDto.orderId(),
                LocalDateTime.now(), createPaymentDto.amount(), PaymentStatus.PENDING
        );

        PaymentEventDto paymentEventDto = new PaymentEventDto(
                savedPayment.getId(),
                savedPayment.getUserId(),
                savedPayment.getOrderId(),
                savedPayment.getStatus()
        );

        GetPaymentDto expectedResult = new GetPaymentDto(
                savedPayment.getId(),
                savedPayment.getOrderId(),
                savedPayment.getUserId(),
                savedPayment.getStatus(),
                savedPayment.getTimestamp(),
                savedPayment.getAmount()
        );

        when(getPaymentEventMapper.toDto(any(Payment.class))).thenReturn(paymentEventDto);
        when(getPaymentMapper.toDto(any(Payment.class))).thenReturn(expectedResult);

        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        doNothing().when(paymentProducer).sendPaymentCreatedEvent(any(PaymentEventDto.class));

        GetPaymentDto result = paymentService.createPayment(createPaymentDto);

        assertThat(result.status()).isEqualTo(EXPECTED_PAYMENT_STATUS);

        verify(getPaymentEventMapper, times(1)).toDto(any(Payment.class));
        verify(getPaymentMapper, times(1)).toDto(any(Payment.class));
        verify(paymentProducer, times(1)).sendPaymentCreatedEvent(any(PaymentEventDto.class));
    }
    @Test
    void test_getPaymentById_returnPaymentById_whenResourceBelongTo () {

        String paymentId = "694a6081723088150e7cf74c";

        Payment payment = new Payment(
                paymentId,
                1L, 1L,
                LocalDateTime.now(), new BigDecimal(1000), PaymentStatus.PENDING
        );

        GetPaymentDto getPaymentDto = new GetPaymentDto(
                payment.getId(),
                payment.getOrderId(), payment.getUserId(), payment.getStatus(),
                payment.getTimestamp(), payment.getAmount()
        );

        when(securityHelper.getCurrentUserId()).thenReturn(payment.getUserId());
        when(securityHelper.isAdmin()).thenReturn(false);

        when(paymentRepository.findById(paymentId)).thenReturn(
                Optional.of(payment));
        when(getPaymentMapper.toDto(payment)).thenReturn(getPaymentDto);

        GetPaymentDto result = paymentService.getPaymentById(paymentId);

        assertThat(result).isEqualTo(getPaymentDto);
        verify(getPaymentMapper, times(1)).toDto(payment);
    }

    @Test
    void test_getPaymentById_throwAccessDenied_whenResourceDontBelongTo () {

        String paymentId = "694a6081723088150e7cf74c";

        Payment payment = new Payment(
                paymentId,
                1L, 1L,
                LocalDateTime.now(), new BigDecimal(1000), PaymentStatus.PENDING
        );

        Long userIdFromToken = 2L;

        GetPaymentDto getPaymentDto = new GetPaymentDto(
                payment.getId(),
                payment.getOrderId(), payment.getUserId(), payment.getStatus(),
                payment.getTimestamp(), payment.getAmount()
        );

        when(securityHelper.getCurrentUserId()).thenReturn(userIdFromToken);
        when(securityHelper.isAdmin()).thenReturn(false);

        when(paymentRepository.findById(paymentId)).thenReturn(
                Optional.of(payment));

        assertThatThrownBy(() -> paymentService.getPaymentById(paymentId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You do not have permission to access this resource");
    }

    @Test
    void test_getPaymentById_paymentByIdNotFound () {

        String paymentId = "694a6081723088150e7cf74c";

        Payment payment = new Payment(
                paymentId,
                1L, 1L,
                LocalDateTime.now(), new BigDecimal(1000), PaymentStatus.PENDING
        );

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentById(paymentId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(getPaymentMapper, times(0)).toDto(payment);
    }

    @Test
    void test_getAllPayments_returnAllPayments () {

        Pageable pageable = PageRequest.of(0, 10);

        List<Payment> payments = List.of(
                new Payment(
                        "694a6081723088150e7cf74c",
                        1L, 1L,
                        LocalDateTime.now(), new BigDecimal(1000), PaymentStatus.PENDING)
        );

        Page<Payment> paymentsPage = new PageImpl<>(payments, pageable, payments.size());

        List<GetPaymentDto> getPaymentDtoList = List.of(
                new GetPaymentDto(
                        payments.getFirst().getId(),
                        payments.getFirst().getOrderId(), payments.getFirst().getUserId(),
                        payments.getFirst().getStatus(),
                        payments.getFirst().getTimestamp(), payments.getFirst().getAmount())
        );

        Page<GetPaymentDto> getPaymentDtosPage = new PageImpl<>(getPaymentDtoList, pageable, getPaymentDtoList.size());

        when(paymentRepository.findAll(pageable)).thenReturn(paymentsPage);

        when(getPaymentMapper.toDto(any(Payment.class))).thenReturn(getPaymentDtoList.getFirst());
        Page<GetPaymentDto> result = paymentService.getAllPayments(pageable);

        assertThat(result).isEqualTo(getPaymentDtosPage);

        verify(getPaymentMapper, times(payments.size())).toDto(any(Payment.class));
    }

    @Test
    void test_getPaymentsByUserId_returnPaymentsByUserId_whenResourceBelongTo () {

        Long userId = 1L;

        when(securityHelper.getCurrentUserId()).thenReturn(userId);
        when(securityHelper.isAdmin()).thenReturn(false);

        Pageable pageable = PageRequest.of(0, 10);

        List<Payment> payments = List.of(
                new Payment(
                        "694a6081723088150e7cf74c",
                        userId, 1L,
                        LocalDateTime.now(), new BigDecimal(1000), PaymentStatus.PENDING)
        );

        Page<Payment> paymentsPage = new PageImpl<>(payments, pageable, payments.size());

        List<GetPaymentDto> getPaymentDtoList = List.of(
                new GetPaymentDto(
                        payments.getFirst().getId(),
                        payments.getFirst().getOrderId(), payments.getFirst().getUserId(), payments.getFirst().getStatus(),
                        payments.getFirst().getTimestamp(), payments.getFirst().getAmount())
        );

        Page<GetPaymentDto> getPaymentDtosPage = new PageImpl<>(getPaymentDtoList, pageable, getPaymentDtoList.size());

        when(paymentRepository.findByUserId(userId, pageable)).thenReturn(paymentsPage);

        when(getPaymentMapper.toDto(any(Payment.class))).thenReturn(getPaymentDtoList.get(0));

        Page<GetPaymentDto> result = paymentService.getPaymentsByUserId(userId, pageable);

        assertThat(result).isEqualTo(getPaymentDtosPage);

        verify(getPaymentMapper, times(payments.size())).toDto(any(Payment.class));
    }

    @Test
    void test_getPaymentsByUserId_throwAccessDenied_whenResourceDontBelongTo () {

        Long userId = 1L;
        Long userIdFromToken = 2L;

        when(securityHelper.getCurrentUserId()).thenReturn(userIdFromToken);
        when(securityHelper.isAdmin()).thenReturn(false);

        assertThatThrownBy(() -> paymentService.getPaymentsByUserId(userId, Pageable.unpaged()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You do not have permission to access this resource");
    }

    @Test
    void test_getPaymentsByOrderId_returnPaymentsByOrderId () {

        Long orderId = 1L;

        Pageable pageable = PageRequest.of(0, 10);

        List<Payment> payments = List.of(
                new Payment(
                        "694a6081723088150e7cf74c",
                        1L, orderId,
                        LocalDateTime.now(), new BigDecimal(1000), PaymentStatus.PENDING)
        );

        Page<Payment> paymentsPage = new PageImpl<>(payments, pageable, payments.size());

        List<GetPaymentDto> getPaymentDtoList = List.of(
                new GetPaymentDto(
                        payments.getFirst().getId(),
                        payments.getFirst().getOrderId(), payments.getFirst().getUserId(),
                        payments.getFirst().getStatus(),
                        payments.getFirst().getTimestamp(), payments.getFirst().getAmount())
        );

        Page<GetPaymentDto> getPaymentDtosPage = new PageImpl<>(getPaymentDtoList, pageable, getPaymentDtoList.size());

        when(paymentRepository.findByOrderId(orderId, pageable)).thenReturn(paymentsPage);

        when(getPaymentMapper.toDto(any(Payment.class))).thenReturn(getPaymentDtoList.getFirst());

        Page<GetPaymentDto> result = paymentService.getPaymentsByOrderId(orderId, pageable);

        assertThat(result).isEqualTo(getPaymentDtosPage);

        verify(getPaymentMapper, times(payments.size())).toDto(any(Payment.class));
    }

    @Test
    void test_getPaymentsByStatus_returnPaymentsByStatuses () {

        List<PaymentStatus> statuses = List.of(PaymentStatus.PENDING);

        Pageable pageable = PageRequest.of(0, 10);

        List<Payment> payments = List.of(
                new Payment(
                        "694a6081723088150e7cf74c",
                        1L, 1L,
                        LocalDateTime.now(), new BigDecimal(1000), PaymentStatus.PENDING)
        );

        Page<Payment> paymentsPage = new PageImpl<>(payments, pageable, payments.size());

        List<GetPaymentDto> getPaymentDtoList = List.of(
                new GetPaymentDto(
                        payments.get(0).getId(),
                        payments.get(0).getOrderId(), payments.get(0).getUserId(), payments.get(0).getStatus(),
                        payments.get(0).getTimestamp(), payments.get(0).getAmount())
        );

        Page<GetPaymentDto> getPaymentDtosPage = new PageImpl<>(getPaymentDtoList, pageable, getPaymentDtoList.size());

        when(paymentRepository.findAllByStatusIn(statuses, pageable)).thenReturn(paymentsPage);

        when(getPaymentMapper.toDto(any(Payment.class))).thenReturn(getPaymentDtoList.get(0));

        Page<GetPaymentDto> result = paymentService.getPaymentsByStatus(statuses, pageable);

        assertThat(result).isEqualTo(getPaymentDtosPage);

        verify(getPaymentMapper, times(payments.size())).toDto(any(Payment.class));
    }

    @Test
    void test_getPaymentTotalResult_returnPaymentTotalResult () {

        LocalDateTime paymentCreationDate = LocalDateTime.now();
        BigDecimal totalResult = new BigDecimal(1000);

        GetPaymentTotalResultDto getPaymentTotalResultDto = new GetPaymentTotalResultDto(
                totalResult
        );

        when(paymentRepository.sumPaymentInPeriod(paymentCreationDate, paymentCreationDate)).thenReturn(getPaymentTotalResultDto);

        BigDecimal result = paymentService.getAllPaymentTotalResult(paymentCreationDate, paymentCreationDate);

        assertThat(result).isEqualTo(getPaymentTotalResultDto.amount());
    }

    @Test
    void test_deletePayment_deletePaymentById () {

        String paymentId = "694a6081723088150e7cf74c";

        doNothing().when(paymentRepository).deleteById(paymentId);

        paymentService.deletePayment(paymentId);

        verify(paymentRepository, times(1)).deleteById(paymentId);
    }

    @Test
    void test_getTotalPaymentResultForCurrentUser_success(){
        Long userId = 1L;
        when(securityHelper.getCurrentUserId()).thenReturn(userId);
        when(securityHelper.isAdmin()).thenReturn(false);
        BigDecimal total = new BigDecimal(1000);
        GetPaymentTotalResultDto getPaymentTotalResultDto = new GetPaymentTotalResultDto(
                total
        );
        when(paymentRepository.sumPaymentByUserInPeriod(
                eq(userId),
                any(LocalDateTime.class),
                any(LocalDateTime.class))).thenReturn(getPaymentTotalResultDto);
        BigDecimal result = paymentService
                .getAllPaymentTotalResultForCurrentUser(userId,
                        LocalDateTime.now(),LocalDateTime.MAX);
        assertThat(result).isEqualTo(getPaymentTotalResultDto.amount());
    }
}
