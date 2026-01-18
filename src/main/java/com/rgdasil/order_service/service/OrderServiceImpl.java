package com.rgdasil.order_service.service;

import com.rgdasil.order_service.client.AuthServiceClient;
import com.rgdasil.order_service.client.CartServiceClient;
import com.rgdasil.order_service.client.ProductServiceClient;
import com.rgdasil.order_service.domain.Order;
import com.rgdasil.order_service.domain.OrderItem;
import com.rgdasil.order_service.domain.OrderStatus;
import com.rgdasil.order_service.dto.external.AuthResponse;
import com.rgdasil.order_service.dto.external.CartDTO;
import com.rgdasil.order_service.dto.external.CartItemDTO;
import com.rgdasil.order_service.dto.external.ProductDTO;
import com.rgdasil.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final AuthServiceClient authServiceClient;
    private final CartServiceClient cartServiceClient;
    private final ProductServiceClient productServiceClient;

    @Override
    @Transactional
    public Order createOrder(String token, String idempotencyKey) {
        // 1. Verifica Idempotência
        if (idempotencyKey != null) {
            Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existingOrder.isPresent()) {
                log.info("Pedido duplicado detectado (Idempotency Key: {}). Retornando pedido existente.", idempotencyKey);
                return existingOrder.get();
            }
        }

        // 2. Valida Token no Auth-Service
        AuthResponse authResponse = authServiceClient.validateToken(token);
        if (!authResponse.isValid()) {
            throw new IllegalArgumentException("Token inválido ou expirado.");
        }
        String userId = authResponse.getUserId();

        // 3. Busca Carrinho
        CartDTO cart = cartServiceClient.getCart(token)
                .orElseThrow(() -> new IllegalArgumentException("Carrinho vazio ou não encontrado."));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Não é possível criar um pedido sem itens.");
        }

        // 4. Valida Estoque
        for (CartItemDTO cartItem : cart.getItems().values()) {
            ProductDTO product = productServiceClient.getProductById(cartItem.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + cartItem.getProductId()));

            if (product.getStock() < cartItem.getQuantity()) {
                throw new IllegalArgumentException("Estoque insuficiente para o produto: " + product.getName());
            }
        }

        // 5. Construi o Pedido
        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.CONFIRMED)
                .idempotencyKey(idempotencyKey)
                .items(new ArrayList<>())
                .totalPrice(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;

        // 6. Converte Itens e Calcular Total
        for (CartItemDTO cartItem : cart.getItems().values()) {
            OrderItem orderItem = OrderItem.builder()
                    .productId(cartItem.getProductId())
                    .productName(cartItem.getProductName())
                    .quantity(cartItem.getQuantity())
                    .price(cartItem.getPrice())
                    .build();
            
            order.addItem(orderItem);
            
            BigDecimal itemTotal = cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            total = total.add(itemTotal);
        }
        order.setTotalPrice(total);

        // 7. Salva Pedido no Banco
        Order savedOrder = orderRepository.save(order);
        log.info("Pedido criado com sucesso: ID {}", savedOrder.getId());

        // 8. Baixa Estoque
        try {
            for (OrderItem item : savedOrder.getItems()) {
                productServiceClient.reduceStock(item.getProductId(), item.getQuantity());
            }
        } catch (Exception e) {
            log.error("ERRO CRÍTICO: Falha ao dar baixa no estoque após salvar pedido. Rollback acionado.", e);
            throw e; 
        }

        cartServiceClient.clearCart(token);

        return savedOrder;
    }
}