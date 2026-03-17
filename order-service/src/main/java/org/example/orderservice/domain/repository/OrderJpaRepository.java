package org.example.orderservice.domain.repository;

import org.example.orderservice.domain.models.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;import org.springframework.stereotype.Repository;

@Repository
public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {
}
