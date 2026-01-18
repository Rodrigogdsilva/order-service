package com.rgdasil.order_service.repository;

import com.rgdasil.order_service.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByUserId(String userId);
    
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
}