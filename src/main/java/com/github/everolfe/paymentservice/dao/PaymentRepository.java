package com.github.everolfe.paymentservice.dao;

import com.github.everolfe.paymentservice.dto.GetPaymentTotalResultDto;
import com.github.everolfe.paymentservice.entity.Payment;
import com.github.everolfe.paymentservice.entity.PaymentStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentRepository extends MongoRepository<Payment, String> {

    Page<Payment> findByOrderId(Long orderId, Pageable pageable);

    Page<Payment> findByUserId(Long userId, Pageable pageable);

    Page<Payment> findAllByStatusIn(Collection<PaymentStatus> statuses, Pageable pageable);

    @Aggregation(pipeline = {
            "{ $match:  {timestamp: {$gte:  ?0, $lte:  ?1 } } }",
            "{ $group: { _id: null, total: { $sum: '$amount'} } }"
    })
    GetPaymentTotalResultDto sumPaymentInPeriod(LocalDateTime startDate, LocalDateTime endDate);

    @Aggregation(pipeline = {
            "{ $match:  {user_id: ?0, timestamp: {$gte: ?1, $lte: ?2 } } }",
            "{ $group: { _id: null, total: { $sum: '$amount'} } }"
    })
    GetPaymentTotalResultDto sumPaymentByUserInPeriod(Long userId, LocalDateTime startDate, LocalDateTime endDate);
}
