package com.github.everolfe.paymentservice.integration;

import com.github.everolfe.paymentservice.dao.PaymentRepository;
import com.github.everolfe.paymentservice.dto.CreatePaymentDto;
import com.github.everolfe.paymentservice.dto.GetPaymentDto;
import com.github.everolfe.paymentservice.dto.PaymentEventDto;
import com.github.everolfe.paymentservice.entity.Payment;
import com.github.everolfe.paymentservice.entity.PaymentStatus;
import com.github.everolfe.paymentservice.service.PaymentService;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;


class PaymentServiceImplIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    private Consumer<String, PaymentEventDto> testConsumer;

    @BeforeEach
    void setup() {

        stubFor(any(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("42"))); // SUCCESS
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                kafkaContainer.getBootstrapServers(),
                "test-group",
                "true");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        JsonDeserializer<PaymentEventDto> deserializer = new JsonDeserializer<>(PaymentEventDto.class);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);

        DefaultKafkaConsumerFactory<String, PaymentEventDto> cf =
                new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), deserializer);

        testConsumer = cf.createConsumer();
        testConsumer.subscribe(Collections.singletonList("create-payment"));
    }

    @AfterEach
    void cleanup() {
        paymentRepository.deleteAll();
        if (testConsumer != null) {
            testConsumer.close();
        }
    }

    @Test
    void shouldCreatePaymentAndSendEvent() {

        Long orderId = 123L;
        Long userId = 456L;
        BigDecimal amount = new BigDecimal("100.50");

        CreatePaymentDto dto = new CreatePaymentDto(userId, orderId, amount);

        GetPaymentDto result = paymentService.createPayment(dto);

        List<Payment> payments = paymentRepository.findAll();
        assertThat(payments).hasSize(1);

        Payment payment = payments.get(0);
        assertThat(payment.getOrderId()).isEqualTo(orderId);
        assertThat(payment.getUserId()).isEqualTo(userId);
        assertThat(payment.getAmount()).isEqualByComparingTo(amount);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

        ConsumerRecord<String, PaymentEventDto> record =
                KafkaTestUtils.getSingleRecord(testConsumer, "create-payment", Duration.ofSeconds(10));

        PaymentEventDto event = record.value();
        assertThat(event).isNotNull();
        assertThat(event.orderId()).isEqualTo(orderId);
        assertThat(event.userId()).isEqualTo(userId);
        assertThat(event.status()).isEqualTo(PaymentStatus.SUCCESS);
    }
}