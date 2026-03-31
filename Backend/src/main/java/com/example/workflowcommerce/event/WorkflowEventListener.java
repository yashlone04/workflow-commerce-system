package com.example.workflowcommerce.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.workflowcommerce.service.workflow.WorkflowIntegrationService;

/**
 * Listens for business events and triggers workflow transitions.
 * Using @TransactionalEventListener(phase = AFTER_COMMIT) ensures
 * the workflow transitions only run AFTER the business transaction commits,
 * making the data visible to the workflow's separate transaction.
 */
@Component
public class WorkflowEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowEventListener.class);

    @Autowired
    private WorkflowIntegrationService workflowIntegrationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        logger.info("Handling PaymentCompletedEvent for order {} (after commit)", event.getOrderId());
        workflowIntegrationService.onPaymentCompleted(event.getOrderId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentRefunded(PaymentRefundedEvent event) {
        logger.info("Handling PaymentRefundedEvent for order {} (after commit)", event.getOrderId());
        workflowIntegrationService.onPaymentRefunded(event.getOrderId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleShippingCreated(ShippingCreatedEvent event) {
        logger.info("Handling ShippingCreatedEvent for order {} (after commit)", event.getOrderId());
        workflowIntegrationService.onShippingCreated(event.getOrderId(), event.getTrackingNumber());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderDelivered(OrderDeliveredEvent event) {
        logger.info("Handling OrderDeliveredEvent for order {} (after commit)", event.getOrderId());
        workflowIntegrationService.onOrderDelivered(event.getOrderId());
    }
}
