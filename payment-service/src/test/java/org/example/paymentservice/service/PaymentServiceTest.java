package org.example.paymentservice.service;

import org.example.commonlibs.api.http.payment.CreatePaymentRequestDto;
import org.example.commonlibs.api.http.payment.CreatePaymentResponseDto;
import org.example.commonlibs.api.http.payment.PaymentMethod;
import org.example.commonlibs.api.http.payment.PaymentStatus;
import org.example.paymentservice.mapper.PaymentEntityMapper;
import org.example.paymentservice.model.PaymentEntity;
import org.example.paymentservice.repository.PaymentJpaRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentJpaRepository repository;
    @Mock private PaymentEntityMapper mapper;

    @InjectMocks
    private PaymentService paymentService;

    @Nested
    class MakePayment {

        @Test
        void returnsExistingPayment_withoutSaving() {
            var existing = entity(1L, PaymentStatus.PAYMENT_SUCCEEDED, PaymentMethod.CARD);
            when(repository.findByOrderId(1L)).thenReturn(Optional.of(existing));
            when(mapper.toResponseDto(existing)).thenReturn(response(1L, PaymentStatus.PAYMENT_SUCCEEDED));

            var result = paymentService.makePayment(request(1L, PaymentMethod.CARD));

            verify(repository, never()).save(any());
            assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.PAYMENT_SUCCEEDED);
        }

        @Test
        void returnsExistingPayment_evenIfMethodDiffers() {
            var existing = entity(1L, PaymentStatus.PAYMENT_SUCCEEDED, PaymentMethod.CARD);
            when(repository.findByOrderId(1L)).thenReturn(Optional.of(existing));
            when(mapper.toResponseDto(existing)).thenReturn(response(1L, PaymentStatus.PAYMENT_SUCCEEDED));

            var result = paymentService.makePayment(request(1L, PaymentMethod.QR));

            verify(repository, never()).save(any());
            assertThat(result.paymentId()).isEqualTo(1L);
        }

        @Test
        void succeedsWithCard() {
            when(repository.findByOrderId(2L)).thenReturn(Optional.empty());
            var newEntity = entity(null, null, PaymentMethod.CARD);
            when(mapper.toEntity(any())).thenReturn(newEntity);
            var saved = entity(10L, PaymentStatus.PAYMENT_SUCCEEDED, PaymentMethod.CARD);
            when(repository.save(any())).thenReturn(saved);
            when(mapper.toResponseDto(saved)).thenReturn(response(10L, PaymentStatus.PAYMENT_SUCCEEDED));

            var result = paymentService.makePayment(request(2L, PaymentMethod.CARD));

            assertThat(newEntity.getPaymentStatus()).isEqualTo(PaymentStatus.PAYMENT_SUCCEEDED);
            assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.PAYMENT_SUCCEEDED);
        }

        @Test
        void failsWithQr() {
            when(repository.findByOrderId(3L)).thenReturn(Optional.empty());
            var newEntity = entity(null, null, PaymentMethod.QR);
            when(mapper.toEntity(any())).thenReturn(newEntity);
            var saved = entity(11L, PaymentStatus.PAYMENT_FAILED, PaymentMethod.QR);
            when(repository.save(any())).thenReturn(saved);
            when(mapper.toResponseDto(saved)).thenReturn(response(11L, PaymentStatus.PAYMENT_FAILED));

            var result = paymentService.makePayment(request(3L, PaymentMethod.QR));

            assertThat(newEntity.getPaymentStatus()).isEqualTo(PaymentStatus.PAYMENT_FAILED);
            assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.PAYMENT_FAILED);
        }

        @Test
        void savesNewEntity_whenNoExistingPayment() {
            when(repository.findByOrderId(4L)).thenReturn(Optional.empty());
            var newEntity = entity(null, null, PaymentMethod.CARD);
            when(mapper.toEntity(any())).thenReturn(newEntity);
            var saved = entity(20L, PaymentStatus.PAYMENT_SUCCEEDED, PaymentMethod.CARD);
            when(repository.save(newEntity)).thenReturn(saved);
            when(mapper.toResponseDto(saved)).thenReturn(response(20L, PaymentStatus.PAYMENT_SUCCEEDED));

            paymentService.makePayment(request(4L, PaymentMethod.CARD));

            verify(repository).save(newEntity);
        }

        @Test
        void mapsRequestToEntity() {
            when(repository.findByOrderId(5L)).thenReturn(Optional.empty());
            var newEntity = entity(null, null, PaymentMethod.CARD);
            when(mapper.toEntity(any())).thenReturn(newEntity);
            var saved = entity(21L, PaymentStatus.PAYMENT_SUCCEEDED, PaymentMethod.CARD);
            when(repository.save(any())).thenReturn(saved);
            when(mapper.toResponseDto(saved)).thenReturn(response(21L, PaymentStatus.PAYMENT_SUCCEEDED));

            paymentService.makePayment(request(5L, PaymentMethod.CARD));

            verify(mapper).toEntity(any());
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private CreatePaymentRequestDto request(Long orderId, PaymentMethod method) {
        return CreatePaymentRequestDto.builder()
                .orderId(orderId)
                .paymentMethod(method)
                .amount(BigDecimal.valueOf(500))
                .build();
    }

    private PaymentEntity entity(Long id, PaymentStatus status, PaymentMethod method) {
        var e = new PaymentEntity();
        e.setId(id);
        e.setOrderId(1L);
        e.setAmount(BigDecimal.valueOf(500));
        e.setPaymentStatus(status);
        e.setPaymentMethod(method);
        return e;
    }

    private CreatePaymentResponseDto response(Long paymentId, PaymentStatus status) {
        return new CreatePaymentResponseDto(paymentId, status, 1L, PaymentMethod.CARD, BigDecimal.valueOf(500));
    }
}
