package org.example.deliveryservice.kafka;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commonlibs.api.kafka.OrderPaidEvent;
import org.example.deliveryservice.domain.service.DeliveryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;

@Slf4j
@EnableKafka
@Configuration
@ConditionalOnProperty(prefix = "spring.kafka", name = "bootstrap-servers", havingValue = "localhost:9092")
@AllArgsConstructor
public class OrderPaidKafkaConsumer {

    private final DeliveryService deliveryService;

    @KafkaListener(
            topics = "${order-paid-topic}",
            containerFactory = "orderPaidEventListenerFactory"
    )
    public void listen(OrderPaidEvent event) {
        log.info("Received order paid event: {}", event);
        deliveryService.processOrderPaid(event);
    }
}
