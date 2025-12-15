package com.rgdasil.order_service.dto.external;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class CartItemDTO {
    private String productId;
    private String productName;
    private Integer quantity;
    private BigDecimal price;
}