package com.rgdasil.order_service.client;

import com.rgdasil.order_service.dto.external.CartDTO;
import com.rgdasil.order_service.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceClient {

    private final RestTemplate restTemplate;

    @Value("${service.cart.url}")
    private String cartServiceUrl;

    @CircuitBreaker(name = "cartService", fallbackMethod = "getCartFallback")
    public Optional<CartDTO> getCart(String token) {
        HttpHeaders headers = new HttpHeaders();
        
        String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;
        headers.set("Authorization", authHeader);
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = UriComponentsBuilder.fromHttpUrl(cartServiceUrl)
                .path("/cart") 
                .toUriString();

        log.info("Buscando carrinho em: {}", url);

        try {
            ResponseEntity<CartDTO> response = restTemplate.exchange(
                    url, 
                    HttpMethod.GET, 
                    entity, 
                    CartDTO.class
            );
            return Optional.ofNullable(response.getBody());
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Carrinho não encontrado (404) para o token fornecido.");
            return Optional.empty();
        } catch (Exception e) {
            log.error("Erro ao buscar carrinho: {}", e.getMessage());
            throw e;
        }
    }

    @CircuitBreaker(name = "cartService", fallbackMethod = "clearCartFallback")
    public void clearCart(String token) {
        HttpHeaders headers = new HttpHeaders();
        String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;
        headers.set("Authorization", authHeader);
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = UriComponentsBuilder.fromHttpUrl(cartServiceUrl)
                .path("/cart")
                .toUriString();

        try {
            restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
            log.info("Carrinho limpo com sucesso.");
        } catch (Exception e) {
            log.error("Erro ao limpar carrinho: {}", e.getMessage());
        }
    }

    // --- Fallbacks ---

    public Optional<CartDTO> getCartFallback(String token, Throwable t) {
        log.error("Fallback getCart ativado. Causa: {}", t.getMessage());
        throw new ServiceUnavailableException("Serviço de carrinho indisponível.");
    }

    public void clearCartFallback(String token, Throwable t) {
        log.error("Fallback clearCart ativado: {}", t.getMessage());
    }
}