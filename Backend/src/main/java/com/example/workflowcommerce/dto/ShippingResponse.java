package com.example.workflowcommerce.dto;

import com.example.workflowcommerce.model.Shipping;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ShippingResponse {
    private Long id;
    private Long orderId;
    private String courierService;
    private String trackingNumber;
    private String shippingStatus;
    private String shippingMethod;
    private BigDecimal shippingCost;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ShippingResponse() {}

    public ShippingResponse(Shipping shipping) {
        this.id = shipping.getId();
        this.orderId = shipping.getOrder().getOrderId();
        this.courierService = shipping.getCourierService();
        this.trackingNumber = shipping.getTrackingNumber();
        this.shippingStatus = shipping.getShippingStatus();
        this.shippingMethod = shipping.getShippingMethod();
        this.shippingCost = shipping.getShippingCost();
        this.createdAt = shipping.getCreatedAt();
        this.updatedAt = shipping.getUpdatedAt();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getCourierService() { return courierService; }
    public void setCourierService(String courierService) { this.courierService = courierService; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public String getShippingStatus() { return shippingStatus; }
    public void setShippingStatus(String shippingStatus) { this.shippingStatus = shippingStatus; }

    public String getShippingMethod() { return shippingMethod; }
    public void setShippingMethod(String shippingMethod) { this.shippingMethod = shippingMethod; }

    public BigDecimal getShippingCost() { return shippingCost; }
    public void setShippingCost(BigDecimal shippingCost) { this.shippingCost = shippingCost; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
