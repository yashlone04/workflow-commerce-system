package com.example.workflowcommerce.payload.request;

import jakarta.validation.constraints.*;

public class OrderItemRequest {
    
    @NotNull
    private Long productId;
    
    @NotNull
    @Min(value = 1)
    private Integer quantity;

    // Getters and Setters
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
