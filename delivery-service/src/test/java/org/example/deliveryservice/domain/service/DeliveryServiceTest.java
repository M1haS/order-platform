package org.example.deliveryservice.domain.service;

import org.example.commonlibs.api.http.payment.PaymentMethod;
import org.example.commonlibs.api.kafka.DeliveryAssignedEvent;
import org.example.commonlibs.api.kafka.OrderPaidEvent;
import org.example.deliveryservice.domain.model.DeliveryEntity;
import org.example.deliveryservice.domain.repository.DeliveryJpaRepository;
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

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock private DeliveryJpaRepository repository;
    @Mock @SuppressWarnings("unchecked")
    private KafkaTemplate<Long, DeliveryAssignedEvent> kafkaTemplate;

    @InjectMocks
    private DeliveryService deliveryService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(deliveryService, "deliveryAssignedTopic", "delivery.events");
    }

    @Nested
    class ProcessOrderPaid {

        @Test
        void skipsProcessing_whenDeliveryAlreadyExists() {
            when(repository.findByOrderId(10L)).thenReturn(Optional.of(deliveryEntity(1L, 10L)));

            deliveryService.processOrderPaid(paidEvent(10L));

            verify(repository, never()).save(any());
            verifyNoInteractions(kafkaTemplate);
        }

        @Test
        void savesNewDelivery_whenNoExistingDelivery() {
            when(repository.findByOrderId(20L)).thenReturn(Optional.empty());
            when(repository.save(any())).thenReturn(deliveryEntity(5L, 20L));
            stubKafka();

            deliveryService.processOrderPaid(paidEvent(20L));

            verify(repository).save(any());
        }

        @Test
        void setsOrderId_onNewDelivery() {
            when(repository.findByOrderId(30L)).thenReturn(Optional.empty());
            when(repository.save(any())).thenReturn(deliveryEntity(6L, 30L));
            stubKafka();

            deliveryService.processOrderPaid(paidEvent(30L));

            ArgumentCaptor<DeliveryEntity> captor = ArgumentCaptor.forClass(DeliveryEntity.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getOrderId()).isEqualTo(30L);
        }

        @Test
        void setsCourierName_onNewDelivery() {
            when(repository.findByOrderId(40L)).thenReturn(Optional.empty());
            when(repository.save(any())).thenReturn(deliveryEntity(7L, 40L));
            stubKafka();

            deliveryService.processOrderPaid(paidEvent(40L));

            ArgumentCaptor<DeliveryEntity> captor = ArgumentCaptor.forClass(DeliveryEntity.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getCourierName()).startsWith("courier-");
        }

        @Test
        void setsEtaMinutesInExpectedRange_onNewDelivery() {
            when(repository.findByOrderId(50L)).thenReturn(Optional.empty());
            when(repository.save(any())).thenReturn(deliveryEntity(8L, 50L));
            stubKafka();

            deliveryService.processOrderPaid(paidEvent(50L));

            ArgumentCaptor<DeliveryEntity> captor = ArgumentCaptor.forClass(DeliveryEntity.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getEtaMinutes()).isBetween(10, 44);
        }

        @Test
        void sendsKafkaEvent_withCorrectOrderId() {
            when(repository.findByOrderId(60L)).thenReturn(Optional.empty());
            when(repository.save(any())).thenReturn(deliveryEntity(9L, 60L));
            stubKafka();

            deliveryService.processOrderPaid(paidEvent(60L));

            ArgumentCaptor<DeliveryAssignedEvent> captor = ArgumentCaptor.forClass(DeliveryAssignedEvent.class);
            verify(kafkaTemplate).send(eq("delivery.events"), eq(60L), captor.capture());
            assertThat(captor.getValue().orderId()).isEqualTo(60L);
        }

        @Test
        void kafkaEventContainsCourierName() {
            when(repository.findByOrderId(70L)).thenReturn(Optional.empty());
            var saved = deliveryEntity(10L, 70L);
            saved.setCourierName("courier-55");
            saved.setEtaMinutes(25);
            when(repository.save(any())).thenReturn(saved);
            stubKafka();

            deliveryService.processOrderPaid(paidEvent(70L));

            ArgumentCaptor<DeliveryAssignedEvent> captor = ArgumentCaptor.forClass(DeliveryAssignedEvent.class);
            verify(kafkaTemplate).send(anyString(), anyLong(), captor.capture());
            assertThat(captor.getValue().courierName()).isEqualTo("courier-55");
            assertThat(captor.getValue().etaMinutes()).isEqualTo(25);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private OrderPaidEvent paidEvent(Long orderId) {
        return OrderPaidEvent.builder()
                .orderId(orderId)
                .paymentId(1L)
                .paymentMethod(PaymentMethod.CARD)
                .amount(BigDecimal.valueOf(500))
                .build();
    }

    private DeliveryEntity deliveryEntity(Long id, Long orderId) {
        var e = new DeliveryEntity();
        e.setId(id);
        e.setOrderId(orderId);
        e.setCourierName("courier-1");
        e.setEtaMinutes(20);
        return e;
    }

    @SuppressWarnings("unchecked")
    private void stubKafka() {
        when(kafkaTemplate.send(anyString(), anyLong(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        mock(org.springframework.kafka.support.SendResult.class)));
    }
}
