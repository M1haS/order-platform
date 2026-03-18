package org.example.orderservice.domain.service;

import org.example.commonlibs.api.http.order.CreateOrderRequestDto;
import org.example.commonlibs.api.http.order.OrderItemDto;
import org.example.commonlibs.api.http.order.OrderStatus;
import org.example.commonlibs.api.http.payment.CreatePaymentRequestDto;
import org.example.commonlibs.api.http.payment.CreatePaymentResponseDto;
import org.example.commonlibs.api.http.payment.PaymentMethod;
import org.example.commonlibs.api.http.payment.PaymentStatus;
import org.example.commonlibs.api.kafka.DeliveryAssignedEvent;
import org.example.commonlibs.api.kafka.OrderPaidEvent;
import org.example.orderservice.domain.external.PaymentHttpClient;
import org.example.orderservice.domain.mapper.OrderEntityMapper;
import org.example.orderservice.domain.models.OrderEntity;
import org.example.orderservice.domain.models.OrderItemEntity;
import org.example.orderservice.domain.models.OrderPaymentRequest;
import org.example.orderservice.domain.repository.OrderJpaRepository;
import org.example.orderservice.domain.sevice.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderJpaRepository orderJpaRepository;
    @Mock private OrderEntityMapper orderEntityMapper;
    @Mock private PaymentHttpClient paymentHttpClient;
    @Mock @SuppressWarnings("unchecked")
    private KafkaTemplate<Long, OrderPaidEvent> kafkaTemplate;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "orderPaidTopic", "orders.event");
    }

    @Nested
    class Create {

        @Test
        void setsStatusToPendingPayment() {
            var entity = entityWithItems(1);
            when(orderEntityMapper.toEntity(any())).thenReturn(entity);
            when(orderJpaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var result = orderService.create(request(1));

            assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        }

        @Test
        void calculatesTotalAmountGreaterThanZero() {
            var entity = entityWithItems(2);
            when(orderEntityMapper.toEntity(any())).thenReturn(entity);
            when(orderJpaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var result = orderService.create(request(2));

            assertThat(result.getTotalAmount()).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        void setsPriceOnEachItem() {
            var entity = entityWithItems(3);
            when(orderEntityMapper.toEntity(any())).thenReturn(entity);
            when(orderJpaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            orderService.create(request(3));

            entity.getItems().forEach(item ->
                    assertThat(item.getPriceAtPurchase()).isGreaterThan(BigDecimal.ZERO));
        }

        @Test
        void savesOrderToRepository() {
            var entity = entityWithItems(1);
            when(orderEntityMapper.toEntity(any())).thenReturn(entity);
            when(orderJpaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            orderService.create(request(1));

            verify(orderJpaRepository).save(entity);
        }

        @Test
        void worksWithEmptyItemSet() {
            var entity = entityWithItems(0);
            when(orderEntityMapper.toEntity(any())).thenReturn(entity);
            when(orderJpaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var result = orderService.create(request(0));

            assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    class GetOrderOrThrow {

        @Test
        void returnsOrder_whenFound() {
            var entity = orderEntity(1L, OrderStatus.PENDING_PAYMENT);
            when(orderJpaRepository.findById(1L)).thenReturn(Optional.of(entity));

            assertThat(orderService.getOrderOrThrow(1L)).isEqualTo(entity);
        }

        @Test
        void throws404_whenNotFound() {
            when(orderJpaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderOrThrow(99L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        void errorMessageContainsId() {
            when(orderJpaRepository.findById(42L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderOrThrow(42L))
                    .hasMessageContaining("42");
        }
    }

    @Nested
    class ProcessPayment {

        @Test
        void setsPaidStatus_whenPaymentSucceeded() {
            var entity = orderEntity(1L, OrderStatus.PENDING_PAYMENT);
            when(orderJpaRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(paymentHttpClient.createPayment(any())).thenReturn(response(PaymentStatus.PAYMENT_SUCCEEDED));
            when(orderJpaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            stubKafka();

            var result = orderService.processPayment(1L, new OrderPaymentRequest(PaymentMethod.CARD));

            assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        }

        @Test
        void setsPaymentFailedStatus_whenPaymentFailed() {
            var entity = orderEntity(1L, OrderStatus.PENDING_PAYMENT);
            when(orderJpaRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(paymentHttpClient.createPayment(any())).thenReturn(response(PaymentStatus.PAYMENT_FAILED));
            when(orderJpaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var result = orderService.processPayment(1L, new OrderPaymentRequest(PaymentMethod.QR));

            assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        }

        @Test
        void throws_whenOrderNotInPendingPayment() {
            var entity = orderEntity(1L, OrderStatus.PAID);
            when(orderJpaRepository.findById(1L)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() ->
                    orderService.processPayment(1L, new OrderPaymentRequest(PaymentMethod.CARD)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("PENDING_PAYMENT");
        }

        @Test
        void sendsKafkaEvent_whenPaymentSucceeded() {
            var entity = orderEntity(42L, OrderStatus.PENDING_PAYMENT);
            when(orderJpaRepository.findById(42L)).thenReturn(Optional.of(entity));
            when(paymentHttpClient.createPayment(any())).thenReturn(response(PaymentStatus.PAYMENT_SUCCEEDED));
            when(orderJpaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            stubKafka();

            orderService.processPayment(42L, new OrderPaymentRequest(PaymentMethod.CARD));

            ArgumentCaptor<OrderPaidEvent> captor = ArgumentCaptor.forClass(OrderPaidEvent.class);
            verify(kafkaTemplate).send(eq("orders.event"), eq(42L), captor.capture());
            assertThat(captor.getValue().orderId()).isEqualTo(42L);
            assertThat(captor.getValue().paymentId()).isEqualTo(999L);
        }

        @Test
        void doesNotSendKafkaEvent_whenPaymentFailed() {
            var entity = orderEntity(1L, OrderStatus.PENDING_PAYMENT);
            when(orderJpaRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(paymentHttpClient.createPayment(any())).thenReturn(response(PaymentStatus.PAYMENT_FAILED));
            when(orderJpaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            orderService.processPayment(1L, new OrderPaymentRequest(PaymentMethod.QR));

            verifyNoInteractions(kafkaTemplate);
        }

        @Test
        void passesCorrectAmountToPaymentClient() {
            var entity = orderEntity(1L, OrderStatus.PENDING_PAYMENT);
            entity.setTotalAmount(BigDecimal.valueOf(1234.56));
            when(orderJpaRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(paymentHttpClient.createPayment(any())).thenReturn(response(PaymentStatus.PAYMENT_SUCCEEDED));
            when(orderJpaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            stubKafka();

            orderService.processPayment(1L, new OrderPaymentRequest(PaymentMethod.CARD));

            ArgumentCaptor<CreatePaymentRequestDto> captor = ArgumentCaptor.forClass(CreatePaymentRequestDto.class);
            verify(paymentHttpClient).createPayment(captor.capture());
            assertThat(captor.getValue().amount()).isEqualByComparingTo(BigDecimal.valueOf(1234.56));
            assertThat(captor.getValue().orderId()).isEqualTo(1L);
        }

        @Test
        void savesEntityAfterPayment() {
            var entity = orderEntity(1L, OrderStatus.PENDING_PAYMENT);
            when(orderJpaRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(paymentHttpClient.createPayment(any())).thenReturn(response(PaymentStatus.PAYMENT_SUCCEEDED));
            when(orderJpaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            stubKafka();

            orderService.processPayment(1L, new OrderPaymentRequest(PaymentMethod.CARD));

            verify(orderJpaRepository).save(entity);
        }
    }

    @Nested
    class ProcessDeliveryAssigned {

        @Test
        void setsDeliveryAssignedStatus_whenOrderIsPaid() {
            var entity = orderEntity(1L, OrderStatus.PAID);
            when(orderJpaRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(orderJpaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            orderService.processDeliveryAssigned(deliveryEvent(1L, "courier-7", 20));

            assertThat(entity.getOrderStatus()).isEqualTo(OrderStatus.DELIVERY_ASSIGNED);
        }

        @Test
        void setsCourierNameAndEta_whenOrderIsPaid() {
            var entity = orderEntity(1L, OrderStatus.PAID);
            when(orderJpaRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(orderJpaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            orderService.processDeliveryAssigned(deliveryEvent(1L, "courier-42", 35));

            assertThat(entity.getCourierName()).isEqualTo("courier-42");
            assertThat(entity.getEtaMinutes()).isEqualTo(35);
        }

        @Test
        void savesOrder_whenOrderIsPaid() {
            var entity = orderEntity(1L, OrderStatus.PAID);
            when(orderJpaRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(orderJpaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            orderService.processDeliveryAssigned(deliveryEvent(1L, "courier-1", 15));

            verify(orderJpaRepository).save(entity);
        }

        @Test
        void doesNotSave_whenOrderAlreadyDeliveryAssigned() {
            var entity = orderEntity(1L, OrderStatus.DELIVERY_ASSIGNED);
            when(orderJpaRepository.findById(1L)).thenReturn(Optional.of(entity));

            orderService.processDeliveryAssigned(deliveryEvent(1L, "courier-9", 15));

            verify(orderJpaRepository, never()).save(any());
        }

        @Test
        void doesNotSave_whenOrderInWrongStatus() {
            var entity = orderEntity(1L, OrderStatus.PENDING_PAYMENT);
            when(orderJpaRepository.findById(1L)).thenReturn(Optional.of(entity));

            orderService.processDeliveryAssigned(deliveryEvent(1L, "courier-5", 10));

            verify(orderJpaRepository, never()).save(any());
        }

        @Test
        void doesNotChangeStatus_whenOrderAlreadyDeliveryAssigned() {
            var entity = orderEntity(1L, OrderStatus.DELIVERY_ASSIGNED);
            when(orderJpaRepository.findById(1L)).thenReturn(Optional.of(entity));

            orderService.processDeliveryAssigned(deliveryEvent(1L, "courier-9", 15));

            assertThat(entity.getOrderStatus()).isEqualTo(OrderStatus.DELIVERY_ASSIGNED);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private OrderEntity orderEntity(Long id, OrderStatus status) {
        var e = new OrderEntity();
        e.setId(id);
        e.setCustomerId(100L);
        e.setAddress("ул. Тестовая 1");
        e.setOrderStatus(status);
        e.setTotalAmount(BigDecimal.valueOf(500));
        e.setItems(new LinkedHashSet<>());
        return e;
    }

    private OrderEntity entityWithItems(int count) {
        var e = new OrderEntity();
        e.setId(1L);
        e.setCustomerId(1L);
        e.setAddress("ул. Тестовая 1");
        var items = new LinkedHashSet<OrderItemEntity>();
        for (int i = 0; i < count; i++) {
            var item = new OrderItemEntity();
            item.setItemId((long) i + 1);
            item.setQuantity(2);
            item.setName("товар-" + i);
            items.add(item);
        }
        e.setItems(items);
        return e;
    }

    private CreateOrderRequestDto request(int itemCount) {
        var items = new HashSet<OrderItemDto>();
        for (int i = 0; i < itemCount; i++) {
            items.add(new OrderItemDto(null, (long) i + 1, 1, null));
        }
        return new CreateOrderRequestDto(1L, "ул. Тестовая 1", items);
    }

    private CreatePaymentResponseDto response(PaymentStatus status) {
        return new CreatePaymentResponseDto(999L, status, 1L, PaymentMethod.CARD, BigDecimal.valueOf(500));
    }

    private DeliveryAssignedEvent deliveryEvent(Long orderId, String courier, int eta) {
        return DeliveryAssignedEvent.builder()
                .orderId(orderId)
                .courierName(courier)
                .etaMinutes(eta)
                .build();
    }

    @SuppressWarnings("unchecked")
    private void stubKafka() {
        when(kafkaTemplate.send(anyString(), anyLong(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        mock(org.springframework.kafka.support.SendResult.class)));
    }
}
