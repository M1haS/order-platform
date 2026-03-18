package org.example.orderservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
        }
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(TestKafkaConfiguration.class)
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=do-not-connect",
        "order-paid-topic=order-paid-test",
        "delivery-assigned-topic=delivery-assigned-test"
})
class OrderServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
