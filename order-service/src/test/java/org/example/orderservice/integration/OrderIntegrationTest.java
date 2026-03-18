package org.example.orderservice.integration;

import org.example.commonlibs.api.http.order.CreateOrderRequestDto;
import org.example.commonlibs.api.http.order.OrderItemDto;
import org.example.commonlibs.api.http.order.OrderStatus;
import org.example.orderservice.domain.models.OrderPaymentRequest;
import org.example.commonlibs.api.http.payment.PaymentMethod;
import org.example.orderservice.domain.repository.OrderJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("orders")
            .withUsername("postgres")
            .withPassword("postgres");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("payment-service.base-url", () -> "http://localhost:9999"); // mock
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @LocalServerPort
    private int port;

    @AfterEach
    void cleanup() {
        orderJpaRepository.deleteAll();
    }

    @Test
    void createOrder_returnsCreatedOrder() {
        var request = new CreateOrderRequestDto(
                1L,
                "ул. Тестовая 1",
                Set.of(new OrderItemDto(null, 10L, 2, null))
        );

        var response = restTemplate.postForEntity(
                "/api/orders", request, Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void createOrder_persistsToDatabase() {
        var request = new CreateOrderRequestDto(
                2L,
                "ул. Тестовая 2",
                Set.of(new OrderItemDto(null, 20L, 1, null))
        );

        restTemplate.postForEntity("/api/orders", request, Object.class);

        assertThat(orderJpaRepository.count()).isEqualTo(1);
    }

    @Test
    void getOrder_returns404_whenNotFound() {
        var response = restTemplate.getForEntity("/api/orders/99999", Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createOrder_setsStatusToPendingPayment() {
        var request = new CreateOrderRequestDto(
                3L,
                "ул. Тестовая 3",
                Set.of(new OrderItemDto(null, 30L, 1, null))
        );

        restTemplate.postForEntity("/api/orders", request, Object.class);

        var orders = orderJpaRepository.findAll();
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getOrderStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }
}
