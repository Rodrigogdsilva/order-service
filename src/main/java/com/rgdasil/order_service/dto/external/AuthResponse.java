package com.rgdasil.order_service.dto.external;

import org.springframework.lang.NonNull;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AuthResponse {
	
    private boolean isValid;
	
	@NonNull
    private String userId;
}