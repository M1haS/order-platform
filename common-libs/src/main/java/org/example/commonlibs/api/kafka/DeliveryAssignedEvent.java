package org.example.commonlibs.api.kafka;

import lombok.Builder;

@Builder
public record DeliveryAssignedEvent(
    Long orderId,
    Long courierId,
    String courierName,
    Integer etaMinutes
) { }
