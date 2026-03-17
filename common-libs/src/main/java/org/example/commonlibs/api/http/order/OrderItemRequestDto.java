package org.example.commonlibs.api.http.order;

public record OrderItemRequestDto(
    Long itemId,
    Integer quantity,
    String name
) { }
