package org.example.orderservice.domain.mapper;

import org.example.orderservice.domain.dto.CreateOrderRequestDto;
import org.example.orderservice.domain.dto.OrderDto;
import org.example.orderservice.domain.dto.OrderItemDto;
import org.example.orderservice.domain.models.OrderEntity;
import org.example.orderservice.domain.models.OrderItemEntity;
import org.mapstruct.*;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderEntityMapper {

    @Mapping(target = "id", ignore = true)
    OrderEntity toEntity(CreateOrderRequestDto orderDto);

    @AfterMapping
    default void linkOrderItemEntities(@MappingTarget OrderEntity orderEntity) {
        orderEntity
                .getItems()
                .forEach(orderItemEntity -> orderItemEntity.setOrder(orderEntity));
    }

    @Mapping(target = "orderItems", source = "items")
    OrderDto toOrderDto(OrderEntity orderEntity);

    @Named("mapOrderItems")
    default Set<OrderItemEntity> mapOrderItems(Set<OrderItemDto> orderItems) {
        if (orderItems == null) {
            return null;
        }
        return orderItems.stream()
                .map(this::toOrderItemEntity)
                .collect(Collectors.toSet());
    }

    @Mapping(target = "order", ignore = true)
    OrderItemEntity toOrderItemEntity(OrderItemDto orderItemDto);

    default Set<OrderItemDto> toOrderItemDtos(Set<OrderItemEntity> orderItemEntities) {
        if (orderItemEntities == null) {
            return null;
        }
        return orderItemEntities.stream()
                .map(this::toOrderItemDto)
                .collect(Collectors.toSet());
    }

    OrderItemDto toOrderItemDto(OrderItemEntity orderItemEntity);
}
