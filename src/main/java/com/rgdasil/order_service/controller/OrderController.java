package com.rgdasil.order_service.controller;

import com.rgdasil.order_service.domain.Order;
import com.rgdasil.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Order> createOrder(
            @RequestHeader("Authorization") String token,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

    	Order createdOrder = orderService.createOrder(token, idempotencyKey);
    	
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }
}