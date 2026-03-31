package com.example.workflowcommerce.event;

import org.springframework.context.ApplicationEvent;

/**
 * Event published after shipping is created for an order.
 */
public class ShippingCreatedEvent extends ApplicationEvent {
    
    private final Long orderId;
    private final String trackingNumber;

    public ShippingCreatedEvent(Object source, Long orderId, String trackingNumber) {
        super(source);
        this.orderId = orderId;
        this.trackingNumber = trackingNumber;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }
}
