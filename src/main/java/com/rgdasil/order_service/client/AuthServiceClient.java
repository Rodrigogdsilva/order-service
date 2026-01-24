package com.rgdasil.order_service.client;

import com.rgdasil.order_service.dto.external.AuthResponse;
import com.rgdasil.order_service.dto.external.AuthRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceClient {

    private final RestTemplate restTemplate;

    @Value("${service.auth.url}")
    private String authServiceUrl;

    @Value("${service.internal.api-key}")
    private String internalApiKey;

    @CircuitBreaker(name = "authService", fallbackMethod = "validateTokenFallback")
    public AuthResponse validateToken(String token) {
        HttpHeaders headers = new HttpHeaders();
        
        headers.set("X-Internal-Api-Key", internalApiKey);
        headers.set("Content-Type", "application/json");

        String authHeaderVal = token.startsWith("Bearer ") ? token : "Bearer " + token;
        headers.set("Authorization", authHeaderVal);

        String cleanToken = token.replace("Bearer ", "").trim();
        AuthRequest request = new AuthRequest(cleanToken); 
        
        HttpEntity<AuthRequest> entity = new HttpEntity<>(request, headers);

        log.info("Validando token no Auth Service: {}", authServiceUrl);

        try {
            @SuppressWarnings("null")
            ResponseEntity<AuthResponse> response = restTemplate.exchange(
                    authServiceUrl,
                    HttpMethod.POST,
                    entity,
                    AuthResponse.class
            );
            return response.getBody();
            
        } catch (Exception e) {
            log.error("Erro ao chamar Auth Service: {}", e.getMessage());
            throw e;
        }
    }

    public AuthResponse validateTokenFallback(String token, Throwable t) {
        log.error("Fallback do Auth Service acionado. Causa: {}", t.getMessage());
        AuthResponse response = new AuthResponse();
        response.setValid(false);
        return response;
    }
}