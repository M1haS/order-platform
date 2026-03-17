package org.example.paymentservice.dto;

import lombok.Builder;
import org.example.paymentservice.model.PaymentMethod;
import org.example.paymentservice.model.PaymentStatus;

import java.math.BigDecimal;

@Builder
public record CreatePaymentRequestDto(
        Long orderId,
        PaymentMethod paymentMethod,
        BigDecimal amount
) { }
