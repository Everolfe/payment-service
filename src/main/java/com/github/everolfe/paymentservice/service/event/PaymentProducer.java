package com.github.everolfe.paymentservice.service.event;

import com.github.everolfe.paymentservice.dto.PaymentEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.producer}")
    private String createPaymentTopic;

    public void sendPaymentCreatedEvent(PaymentEventDto eventDto) {
        log.info("Sending payment created event to kafka: {}", eventDto);
        kafkaTemplate.send(createPaymentTopic, eventDto);
    }
}
