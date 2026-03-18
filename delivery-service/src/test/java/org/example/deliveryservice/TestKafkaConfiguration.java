package org.example.deliveryservice;

import org.example.commonlibs.api.kafka.DeliveryAssignedEvent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestKafkaConfiguration {

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public KafkaTemplate<Long, DeliveryAssignedEvent> kafkaTemplate() {
        return mock(KafkaTemplate.class);
    }
}
