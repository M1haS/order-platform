package org.example.orderservice.domain.dto;

import org.example.orderservice.domain.models.OrderStatus;

import java.math.BigDecimal;
import java.util.Set;

public record OrderDto (
    Long id,
    Long customerId,
    String address,
    BigDecimal totalAmount,
    String courierName,
    Integer etaMinutes,
    OrderStatus orderStatus,
    Set<OrderItemDto> orderItems
) { }
