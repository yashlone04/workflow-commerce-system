package com.example.workflowcommerce.event;

import org.springframework.context.ApplicationEvent;

/**
 * Event published after a payment is completed.
 */
public class PaymentCompletedEvent extends ApplicationEvent {
    
    private final Long orderId;

    public PaymentCompletedEvent(Object source, Long orderId) {
        super(source);
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }
}
