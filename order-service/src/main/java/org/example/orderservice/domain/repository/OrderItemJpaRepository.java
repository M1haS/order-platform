package org.example.orderservice.domain.repository;

import org.example.orderservice.domain.models.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemJpaRepository extends JpaRepository<OrderItemEntity, Long> {
}
