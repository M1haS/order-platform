package org.example.orderservice.domain.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.orderservice.domain.dto.CreateOrderRequestDto;
import org.example.orderservice.domain.dto.OrderDto;
import org.example.orderservice.domain.mapper.OrderEntityMapper;
import org.example.orderservice.domain.sevice.OrderService;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderEntityMapper orderEntityMapper;

    @PostMapping
    public OrderDto create(
            @RequestBody CreateOrderRequestDto request
    ) {
        log.info("Creating order: request={}", request);
        var saved = orderService.create(request);
        return orderEntityMapper.toOrderDto(saved);
    }

    @GetMapping("/{id}")
    public OrderDto getOne(@PathVariable Long id) {
        log.info("Retrieving order with id: {}", id);
        var found = orderService.getOrderOrThrow(id);
        return orderEntityMapper.toOrderDto(found);
    }
}
