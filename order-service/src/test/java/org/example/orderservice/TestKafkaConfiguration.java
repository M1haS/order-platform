package org.example.orderservice;

import org.example.commonlibs.api.kafka.OrderPaidEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestKafkaConfiguration {

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public KafkaTemplate<Long, OrderPaidEvent> kafkaTemplate() {
        return mock(KafkaTemplate.class);
    }
}
