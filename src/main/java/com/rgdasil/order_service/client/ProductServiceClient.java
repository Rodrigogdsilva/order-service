package com.rgdasil.order_service.client;

import com.rgdasil.order_service.dto.external.ProductDTO;
import com.rgdasil.order_service.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceClient {

    private final RestTemplate restTemplate;

    @Value("${service.product.url}")
    private String productServiceUrl;

    @Value("${service.internal.api-key}")
    private String internalApiKey;

    @CircuitBreaker(name = "productService", fallbackMethod = "getProductFallback")
    public Optional<ProductDTO> getProductById(String productId) {
        String url = productServiceUrl + "/" + productId;
        
        try {
            ProductDTO product = restTemplate.getForObject(url, ProductDTO.class);
            return Optional.ofNullable(product);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Produto não encontrado no ProductService: {}", productId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Erro ao buscar produto: {}", e.getMessage());
            throw e;
        }
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "reduceStockFallback")
    public void reduceStock(String productId, int quantity) {
        String url = productServiceUrl + "/" + productId + "/stock";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Api-Key", internalApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Integer> body = Collections.singletonMap("quantity", quantity);
        HttpEntity<Map<String, Integer>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
            log.info("Estoque reduzido para o produto: {}", productId);
        } catch (Exception e) {
            log.error("Falha ao reduzir estoque: {}", e.getMessage());
            throw e;
        }
    }

    // --- Fallbacks ---

    public Optional<ProductDTO> getProductFallback(String productId, Throwable t) {
        log.error("Fallback getProductById ativado para ID {}: {}", productId, t.getMessage());
        throw new ServiceUnavailableException("Serviço de produtos indisponível.");
    }

    public void reduceStockFallback(String productId, int quantity, Throwable t) {
        log.error("Fallback reduceStock ativado para ID {}: {}", productId, t.getMessage());
        throw new ServiceUnavailableException("Não foi possível dar baixa no estoque. Tente novamente.");
    }
}