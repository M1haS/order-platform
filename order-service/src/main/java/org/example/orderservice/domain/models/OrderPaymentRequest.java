package org.example.orderservice.domain.models;

import org.example.commonlibs.api.http.payment.PaymentMethod;

public record OrderPaymentRequest(
        PaymentMethod paymentMethod
) {
}
