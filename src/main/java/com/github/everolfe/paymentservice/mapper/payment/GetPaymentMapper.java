package com.github.everolfe.paymentservice.mapper.payment;

import com.github.everolfe.paymentservice.dto.GetPaymentDto;
import com.github.everolfe.paymentservice.entity.Payment;
import com.github.everolfe.paymentservice.mapper.BaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapper.class)
public interface GetPaymentMapper extends BaseMapper<Payment, GetPaymentDto> {
}
