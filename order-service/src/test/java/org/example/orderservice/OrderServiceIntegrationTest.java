package org.example.orderservice;

import org.example.commonlibs.api.http.order.CreateOrderRequestDto;
import org.example.commonlibs.api.http.order.OrderItemDto;
import org.example.commonlibs.api.http.order.OrderStatus;
import org.example.commonlibs.api.http.payment.PaymentMethod;
import org.example.orderservice.domain.models.OrderPaymentRequest;
import org.example.orderservice.domain.repository.OrderJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("orders")
            .withUsername("postgres")
            .withPassword("postgres");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0")
    );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("payment-service.base-url", () -> "http://localhost:9999");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @BeforeEach
    void setUp() {
        orderJpaRepository.deleteAll();
    }

    @Test
    void createOrder_returnsCreatedOrder() {
        var request = new CreateOrderRequestDto(
                1L,
                "ул. Тестовая 1",
                Set.of(new OrderItemDto(null, 10L, 2, null))
        );

        var response = restTemplate.postForEntity("/api/orders", request, Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void createOrder_persistsToDatabase() {
        var request = new CreateOrderRequestDto(
                2L,
                "пр. Мира 5",
                Set.of(new OrderItemDto(null, 1L, 1, null))
        );

        restTemplate.postForEntity("/api/orders", request, Object.class);

        assertThat(orderJpaRepository.count()).isEqualTo(1);
    }

    @Test
    void getOrder_returns404_whenNotFound() {
        var response = restTemplate.getForEntity("/api/orders/999", Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getOrder_returnsOrder_whenExists() {
        var request = new CreateOrderRequestDto(
                3L,
                "ул. Ленина 1",
                Set.of(new OrderItemDto(null, 5L, 3, null))
        );
        var created = restTemplate.postForEntity("/api/orders", request, java.util.Map.class);
        var id = ((Number) created.getBody().get("id")).longValue();

        var response = restTemplate.getForEntity("/api/orders/" + id, Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void createOrder_setsStatusToPendingPayment() {
        var request = new CreateOrderRequestDto(
                4L,
                "ул. Садовая 10",
                Set.of(new OrderItemDto(null, 7L, 1, null))
        );

        restTemplate.postForEntity("/api/orders", request, Object.class);

        var saved = orderJpaRepository.findAll().get(0);
        assertThat(saved.getOrderStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }
}
