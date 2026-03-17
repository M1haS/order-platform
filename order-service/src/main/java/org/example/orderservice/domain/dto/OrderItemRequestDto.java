package org.example.orderservice.domain.dto;

public record OrderItemRequestDto(
    Long itemId,
    Integer quantity,
    String name
) { }
