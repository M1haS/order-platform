package org.example.orderservice.domain.sevice;

import lombok.RequiredArgsConstructor;
import org.example.commonlibs.api.http.order.CreateOrderRequestDto;
import org.example.commonlibs.api.http.order.OrderStatus;
import org.example.commonlibs.api.http.payment.CreatePaymentRequestDto;
import org.example.commonlibs.api.http.payment.PaymentStatus;
import org.example.orderservice.domain.external.PaymentHttpClient;
import org.example.orderservice.domain.mapper.OrderEntityMapper;
import org.example.orderservice.domain.models.OrderEntity;
import org.example.orderservice.domain.models.OrderItemEntity;
import org.example.orderservice.domain.models.OrderPaymentRequest;
import org.example.orderservice.domain.repository.OrderJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderJpaRepository orderJpaRepository;
    private final OrderEntityMapper orderEntityMapper;
    private final PaymentHttpClient paymentHttpClient;

    public OrderEntity create(CreateOrderRequestDto request) {
        var entity = orderEntityMapper.toEntity(request);
        calculatePricingForOrder(entity);
        entity.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        return orderJpaRepository.save(entity);
    }


    public OrderEntity getOrderOrThrow(Long id) {
        var orderEntityOptional = orderJpaRepository.findById(id);
        return orderEntityOptional
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Entity with id `%s` not found".formatted(id)));
    }

    private void calculatePricingForOrder(OrderEntity entity) {
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (OrderItemEntity item : entity.getItems()) {
            var randomPrice = ThreadLocalRandom.current().nextDouble(100, 5000);
            item.setPriceAtPurchase(BigDecimal.valueOf(randomPrice));

            totalPrice = item.getPriceAtPurchase()
                    .multiply(BigDecimal.valueOf(item.getQuantity()))
                    .add(totalPrice);
        }
        entity.setTotalAmount(totalPrice);
    }

    public OrderEntity processPayment(
            Long id,
            OrderPaymentRequest request
    ) {
        var entity = getOrderOrThrow(id);
        if (!entity.getOrderStatus().equals(OrderStatus.PENDING_PAYMENT))
            throw new RuntimeException("Order must be in status PENDING_PAYMENT");
        var response = paymentHttpClient.createPayment(CreatePaymentRequestDto.builder()
                        .orderId(id)
                        .paymentMethod(request.paymentMethod())
                        .amount(entity.getTotalAmount())
                .build());
        var status = response.paymentStatus().equals(PaymentStatus.PAYMENT_SUCCEEDED)
                ? OrderStatus.PAYMENT_FAILED
                : OrderStatus.PAID;

        entity.setOrderStatus(status);
        return orderJpaRepository.save(entity);
    }
}
