package org.example.orderservice.domain.kafka;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commonlibs.api.kafka.DeliveryAssignedEvent;
import org.example.orderservice.domain.sevice.OrderService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;

@Slf4j
@EnableKafka
@Configuration
@ConditionalOnProperty(prefix = "spring.kafka", name = "bootstrap-servers", havingValue = "localhost:9092")
@AllArgsConstructor
public class DeliveryAssignKafkaConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = "${delivery-assigned-topic}",
            containerFactory = "deliveryAssignedEventListenerFactory"
    )
    public void listen(DeliveryAssignedEvent event) {
        log.info("Received delivery assigned event: {}", event);
        orderService.processDeliveryAssigned(event);
    }
}
