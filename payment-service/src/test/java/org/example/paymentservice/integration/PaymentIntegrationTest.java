package org.example.paymentservice.integration;

import org.example.commonlibs.api.http.payment.CreatePaymentRequestDto;
import org.example.commonlibs.api.http.payment.CreatePaymentResponseDto;
import org.example.commonlibs.api.http.payment.PaymentMethod;
import org.example.commonlibs.api.http.payment.PaymentStatus;
import org.example.paymentservice.repository.PaymentJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("orders")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PaymentJpaRepository paymentJpaRepository;

    @AfterEach
    void cleanup() {
        paymentJpaRepository.deleteAll();
    }

    @Test
    void makePayment_withCard_succeeds() {
        var request = CreatePaymentRequestDto.builder()
                .orderId(1L)
                .paymentMethod(PaymentMethod.CARD)
                .amount(BigDecimal.valueOf(500))
                .build();

        var response = restTemplate.postForEntity("/api/payments", request, CreatePaymentResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().paymentStatus()).isEqualTo(PaymentStatus.PAYMENT_SUCCEEDED);
    }

    @Test
    void makePayment_withQr_fails() {
        var request = CreatePaymentRequestDto.builder()
                .orderId(2L)
                .paymentMethod(PaymentMethod.QR)
                .amount(BigDecimal.valueOf(300))
                .build();

        var response = restTemplate.postForEntity("/api/payments", request, CreatePaymentResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().paymentStatus()).isEqualTo(PaymentStatus.PAYMENT_FAILED);
    }

    @Test
    void makePayment_idempotent_returnsExisting() {
        var request = CreatePaymentRequestDto.builder()
                .orderId(3L)
                .paymentMethod(PaymentMethod.CARD)
                .amount(BigDecimal.valueOf(100))
                .build();

        restTemplate.postForEntity("/api/payments", request, CreatePaymentResponseDto.class);
        restTemplate.postForEntity("/api/payments", request, CreatePaymentResponseDto.class);

        assertThat(paymentJpaRepository.count()).isEqualTo(1);
    }

    @Test
    void makePayment_persistsToDatabase() {
        var request = CreatePaymentRequestDto.builder()
                .orderId(4L)
                .paymentMethod(PaymentMethod.CARD)
                .amount(BigDecimal.valueOf(200))
                .build();

        restTemplate.postForEntity("/api/payments", request, CreatePaymentResponseDto.class);

        assertThat(paymentJpaRepository.count()).isEqualTo(1);
        assertThat(paymentJpaRepository.findByOrderId(4L)).isPresent();
    }
}
