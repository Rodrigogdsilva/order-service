package com.rgdasil.order_service.service;

import com.rgdasil.order_service.domain.Order;

public interface OrderService {
    Order createOrder(String token, String idempotencyKey);
}