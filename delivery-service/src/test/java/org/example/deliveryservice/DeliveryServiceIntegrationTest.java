package org.example.deliveryservice;

import org.example.commonlibs.api.http.payment.PaymentMethod;
import org.example.commonlibs.api.kafka.OrderPaidEvent;
import org.example.deliveryservice.domain.repository.DeliveryJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongSerializer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class DeliveryServiceIntegrationTest {

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
        registry.add("spring.kafka.consumer.group-id", () -> "delivery-test-group");
    }

    @Autowired
    private DeliveryJpaRepository deliveryJpaRepository;

    @Value("${order-paid-topic}")
    private String orderPaidTopic;

    @BeforeEach
    void setUp() {
        deliveryJpaRepository.deleteAll();
    }

    private KafkaTemplate<Long, OrderPaidEvent> buildProducer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    @Test
    void whenOrderPaidEventReceived_deliveryIsCreated() {
        var event = OrderPaidEvent.builder()
                .orderId(100L)
                .paymentId(1L)
                .paymentMethod(PaymentMethod.CARD)
                .amount(BigDecimal.valueOf(500))
                .build();

        buildProducer().send(orderPaidTopic, 100L, event);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(deliveryJpaRepository.findByOrderId(100L)).isPresent()
        );
    }

    @Test
    void whenOrderPaidEventReceivedTwice_onlyOneDeliveryCreated() {
        var event = OrderPaidEvent.builder()
                .orderId(200L)
                .paymentId(2L)
                .paymentMethod(PaymentMethod.CARD)
                .amount(BigDecimal.valueOf(300))
                .build();

        var producer = buildProducer();
        producer.send(orderPaidTopic, 200L, event);
        producer.send(orderPaidTopic, 200L, event);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(deliveryJpaRepository.findAll().stream()
                        .filter(d -> d.getOrderId().equals(200L))
                        .count()).isEqualTo(1)
        );
    }
}
