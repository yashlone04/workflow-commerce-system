package com.example.workflowcommerce.event;

import org.springframework.context.ApplicationEvent;

/**
 * Event published after a payment is refunded.
 */
public class PaymentRefundedEvent extends ApplicationEvent {
    
    private final Long orderId;

    public PaymentRefundedEvent(Object source, Long orderId) {
        super(source);
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }
}
