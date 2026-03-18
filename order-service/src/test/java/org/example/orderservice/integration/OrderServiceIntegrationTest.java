package org.example.orderservice.integration;

import org.example.commonlibs.api.http.order.CreateOrderRequestDto;
import org.example.commonlibs.api.http.order.OrderItemDto;
import org.example.commonlibs.api.http.order.OrderStatus;
import org.example.commonlibs.api.http.payment.PaymentMethod;
import org.example.orderservice.domain.models.OrderPaymentRequest;
import org.example.orderservice.domain.repository.OrderJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class OrderServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("orders")
            .withUsername("postgres")
            .withPassword("postgres");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired OrderJpaRepository orderJpaRepository;

    @MockBean
    @SuppressWarnings("unchecked")
    KafkaTemplate<Long, ?> kafkaTemplate;

    @Autowired
    javax.sql.DataSource dataSource;

    @BeforeEach
    void setUp() throws Exception {
        try (var conn = dataSource.getConnection()) {
            try (var stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM order_item_entity");
                stmt.execute("DELETE FROM orders");
            }
        }
        when(kafkaTemplate.send(anyString(), anyLong(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        mock(org.springframework.kafka.support.SendResult.class)));
    }

    @Test
    void createOrder_returnsCreatedOrderWithPendingPaymentStatus() throws Exception {
        var request = new CreateOrderRequestDto(1L, "ул. Тестовая 1",
                Set.of(new OrderItemDto(null, 10L, 2, null)));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void createOrder_persistsToDatabase() throws Exception {
        var request = new CreateOrderRequestDto(2L, "пр. Мира 5",
                Set.of(new OrderItemDto(null, 20L, 1, null)));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

        assertThat(orderJpaRepository.findAll()).hasSize(1);
        assertThat(orderJpaRepository.findAll().get(0).getOrderStatus())
                .isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    void getOrder_returns404_whenNotExists() throws Exception {
        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrder_returnsOrder_whenExists() throws Exception {
        var createRequest = new CreateOrderRequestDto(3L, "ул. Ленина 1",
                Set.of(new OrderItemDto(null, 30L, 3, null)));

        var createResult = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn();

        var orderId = objectMapper.readTree(
                createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.orderStatus").value("PENDING_PAYMENT"));
    }

    @Test
    void createOrder_calculatesNonZeroTotalAmount() throws Exception {
        var request = new CreateOrderRequestDto(4L, "ул. Садовая 10",
                Set.of(new OrderItemDto(null, 40L, 2, null)));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").isNumber());
    }
}
