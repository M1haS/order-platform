package org.example.orderservice.domain.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commonlibs.api.http.order.CreateOrderRequestDto;
import org.example.commonlibs.api.http.order.OrderDto;
import org.example.orderservice.domain.mapper.OrderEntityMapper;
import org.example.orderservice.domain.models.OrderPaymentRequest;
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

    @PostMapping("/{id}/pay")
    public OrderDto payOrder(
            @PathVariable Long id,
            @RequestBody OrderPaymentRequest request
    ) {
        log.info("Paying order with id={}, request={}", id, request);
        var entity = orderService.processPayment(id, request);
        return orderEntityMapper.toOrderDto(entity);
    }
}
