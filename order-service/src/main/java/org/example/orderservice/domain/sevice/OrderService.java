package org.example.orderservice.domain.sevice;

import lombok.RequiredArgsConstructor;
import org.example.orderservice.domain.dto.CreateOrderRequestDto;
import org.example.orderservice.domain.mapper.OrderEntityMapper;
import org.example.orderservice.domain.models.OrderEntity;
import org.example.orderservice.domain.models.OrderItemEntity;
import org.example.orderservice.domain.models.OrderStatus;
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
}
