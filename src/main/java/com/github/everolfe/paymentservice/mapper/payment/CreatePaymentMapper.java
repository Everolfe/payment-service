package com.github.everolfe.paymentservice.mapper.payment;


import com.github.everolfe.paymentservice.dto.CreatePaymentDto;
import com.github.everolfe.paymentservice.entity.Payment;
import com.github.everolfe.paymentservice.mapper.BaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapper.class)
public interface CreatePaymentMapper extends BaseMapper<Payment, CreatePaymentDto> {
}
