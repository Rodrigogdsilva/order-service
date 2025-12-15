package com.rgdasil.order_service.dto.external;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
public class CartDTO {
    private String userId;
    private Map<String, CartItemDTO> items;
}