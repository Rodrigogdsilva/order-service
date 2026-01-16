package com.rgdasil.order_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class ServiceUnavailableException extends RuntimeException {

	private static final long serialVersionUID = -1616908695038470350L;

	public ServiceUnavailableException(String message) {
        super(message);
    }
}