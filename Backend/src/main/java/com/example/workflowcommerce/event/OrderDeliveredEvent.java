package com.example.workflowcommerce.event;

import org.springframework.context.ApplicationEvent;

/**
 * Event published after an order is delivered.
 */
public class OrderDeliveredEvent extends ApplicationEvent {
    
    private final Long orderId;

    public OrderDeliveredEvent(Object source, Long orderId) {
        super(source);
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }
}
