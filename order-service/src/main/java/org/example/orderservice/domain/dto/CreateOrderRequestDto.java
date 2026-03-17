package org.example.orderservice.domain.dto;

import org.example.orderservice.domain.models.OrderStatus;

import java.math.BigDecimal;
import java.util.Set;

public record CreateOrderRequestDto(
        Long customerId,
        String address,
        Set<OrderItemDto> items
) { }
