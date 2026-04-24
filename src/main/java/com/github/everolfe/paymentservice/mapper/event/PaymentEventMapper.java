package com.github.everolfe.paymentservice.mapper.event;

import com.github.everolfe.paymentservice.dto.PaymentEventDto;
import com.github.everolfe.paymentservice.entity.Payment;
import com.github.everolfe.paymentservice.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = BaseMapper.class)
public interface PaymentEventMapper extends BaseMapper<Payment, PaymentEventDto> {

    @Mapping(source = "id", target = "paymentId")
    PaymentEventDto toDto(Payment payment);
}
