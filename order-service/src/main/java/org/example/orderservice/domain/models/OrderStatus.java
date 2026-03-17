package org.example.orderservice.domain.models;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    PAYMENT_FAILED,
    PENDING_DELIVERY,
    DELIVERED
}
