package com.github.everolfe.paymentservice.entity;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "payments")
public class Payment {

    @Id
    private String id;

    @Field("user_id")
    private Long userId;

    @Field("order_id")
    private Long orderId;

    @CreatedDate
    @Field("timestamp")
    private LocalDateTime timestamp;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal amount;

    @Field("status")
    private PaymentStatus status;
}
