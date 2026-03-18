package org.example.commonlibs.api.kafka;

import lombok.Builder;
import org.example.commonlibs.api.http.payment.PaymentMethod;

import java.math.BigDecimal;

@Builder
public record OrderPaidEvent(
    Long orderId,
    Long paymentId,
    BigDecimal amount,
    PaymentMethod paymentMethod
) { }
