package org.example.paymentservice.dto;

import org.example.paymentservice.model.PaymentMethod;
import org.example.paymentservice.model.PaymentStatus;

import java.math.BigDecimal;

public record CreatePaymentResponseDto(
        Long paymentId,
        PaymentStatus paymentStatus,
        Long orderId,
        PaymentMethod paymentMethod,
        BigDecimal amount
) { }
