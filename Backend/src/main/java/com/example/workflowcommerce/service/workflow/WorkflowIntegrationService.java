package com.example.workflowcommerce.service.workflow;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.workflowcommerce.model.workflow.WorkflowInstance;
import com.example.workflowcommerce.repository.workflow.WorkflowInstanceRepository;

/**
 * Integration service that automatically triggers workflow transitions
 * based on business events (payment, shipping, etc.)
 * 
 * This eliminates the need for manual admin intervention - the workflow
 * automatically progresses as business actions occur.
 * 
 * Uses WorkflowTransitionExecutor with REQUIRES_NEW propagation to run 
 * transitions in separate transactions, preventing workflow failures 
 * from rolling back business transactions.
 */
@Service
@Transactional(readOnly = true)
public class WorkflowIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowIntegrationService.class);

    private static final String WORKFLOW_NAME = "OrderLifecycleWorkflow";
    private static final String ENTITY_TYPE = "ORDER";

    @Autowired
    private WorkflowTransitionExecutor transitionExecutor;

    @Autowired
    private WorkflowInstanceRepository workflowInstanceRepository;

    // ==================== ORDER CREATION ====================

    /**
     * Called when a new order is created.
     * Creates workflow instance and immediately transitions to PAYMENT_PENDING.
     */
    public void onOrderCreated(Long orderId, String createdBy) {
        try {
            logger.info("Order {} created - initializing workflow", orderId);
            
            // Check if workflow instance already exists
            Optional<WorkflowInstance> existing = workflowInstanceRepository
                    .findActiveByEntityWithDetails(ENTITY_TYPE, orderId);
            
            if (existing.isPresent()) {
                logger.info("Workflow instance already exists for order {}", orderId);
                return;
            }

            // Create workflow instance AND transition to PAYMENT_PENDING in same transaction
            transitionExecutor.createWorkflowInstanceWithTransition(
                    WORKFLOW_NAME, ENTITY_TYPE, orderId, createdBy,
                    "PAYMENT_PENDING", "Awaiting customer payment");
            
            logger.info("Workflow instance created and transitioned to PAYMENT_PENDING for order {}", orderId);

        } catch (Exception e) {
            logger.error("Failed to initialize workflow for order {}: {}", orderId, e.getMessage());
            // Don't fail the order creation - workflow is supplementary
        }
    }

    // ==================== PAYMENT EVENTS ====================

    /**
     * Called when payment is initiated (payment record created with Pending status)
     */
    public void onPaymentInitiated(Long orderId) {
        try {
            logger.info("Payment initiated for order {}", orderId);
            // Already in PAYMENT_PENDING, no transition needed
        } catch (Exception e) {
            logger.error("Failed to handle payment initiation for order {}: {}", orderId, e.getMessage());
        }
    }

    /**
     * Called when payment is completed successfully.
     * Transitions workflow from PAYMENT_PENDING → PAID → PROCESSING in one transaction.
     */
    public void onPaymentCompleted(Long orderId) {
        try {
            logger.info("Payment completed for order {} - transitioning to PAID then PROCESSING", orderId);
            
            // Execute both transitions in the same transaction
            transitionExecutor.executeMultipleTransitions(
                    ENTITY_TYPE, orderId,
                    new String[]{"PAID", "PROCESSING"},
                    new String[]{"Payment verified and confirmed", "Order is being processed for fulfillment"}
            );

        } catch (Exception e) {
            logger.error("Failed to transition workflow after payment for order {}: {}", orderId, e.getMessage());
        }
    }

    /**
     * Called when payment fails.
     * Could auto-cancel or leave in PAYMENT_PENDING for retry.
     */
    public void onPaymentFailed(Long orderId) {
        try {
            logger.info("Payment failed for order {}", orderId);
            // For now, leave in PAYMENT_PENDING to allow retry
            // Could auto-cancel after X failed attempts
        } catch (Exception e) {
            logger.error("Failed to handle payment failure for order {}: {}", orderId, e.getMessage());
        }
    }

    /**
     * Called when payment is refunded.
     * Transitions workflow to REFUNDED.
     */
    public void onPaymentRefunded(Long orderId) {
        try {
            logger.info("Payment refunded for order {} - transitioning to REFUNDED", orderId);
            
            transitionExecutor.executeTransition(ENTITY_TYPE, orderId, "REFUNDED", 
                    "SYSTEM", "ROLE_ADMIN", "Payment has been refunded to customer");

        } catch (Exception e) {
            logger.error("Failed to transition workflow after refund for order {}: {}", orderId, e.getMessage());
        }
    }

    // ==================== SHIPPING EVENTS ====================

    /**
     * Called when shipping record is created.
     * Transitions workflow from PROCESSING to SHIPPED.
     */
    public void onShippingCreated(Long orderId, String trackingNumber) {
        try {
            logger.info("Shipping created for order {} with tracking {} - transitioning to SHIPPED", 
                    orderId, trackingNumber);
            
            transitionExecutor.executeTransition(ENTITY_TYPE, orderId, "SHIPPED", 
                    "SYSTEM", "ROLE_ADMIN", "Order shipped. Tracking: " + trackingNumber);

        } catch (Exception e) {
            logger.error("Failed to transition workflow after shipping for order {}: {}", orderId, e.getMessage());
        }
    }

    /**
     * Called when order is delivered.
     * Transitions workflow to DELIVERED (terminal state).
     */
    public void onOrderDelivered(Long orderId) {
        try {
            logger.info("Order {} delivered - transitioning to DELIVERED", orderId);
            
            transitionExecutor.executeTransition(ENTITY_TYPE, orderId, "DELIVERED", 
                    "SYSTEM", "ROLE_ADMIN", "Order has been delivered to customer");

        } catch (Exception e) {
            logger.error("Failed to transition workflow after delivery for order {}: {}", orderId, e.getMessage());
        }
    }

    // ==================== CANCELLATION ====================

    /**
     * Called when order is cancelled.
     * Transitions workflow to CANCELLED.
     */
    public void onOrderCancelled(Long orderId, String reason) {
        try {
            logger.info("Order {} cancelled - transitioning to CANCELLED", orderId);
            
            transitionExecutor.executeTransition(ENTITY_TYPE, orderId, "CANCELLED", 
                    "SYSTEM", "ROLE_ADMIN", "Order cancelled: " + reason);

        } catch (Exception e) {
            logger.error("Failed to transition workflow after cancellation for order {}: {}", orderId, e.getMessage());
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Perform automatic transition with error handling.
     * Uses WorkflowTransitionExecutor to run in a separate transaction.
     */
    private void autoTransition(Long orderId, String targetState, String actor, 
                                 String actorRole, String comment) {
        try {
            Optional<WorkflowInstance> instanceOpt = workflowInstanceRepository
                    .findActiveByEntityWithDetails(ENTITY_TYPE, orderId);
            
            if (instanceOpt.isEmpty()) {
                logger.warn("No active workflow instance found for order {} - skipping transition to {}", 
                        orderId, targetState);
                return;
            }

            WorkflowInstance instance = instanceOpt.get();
            String currentState = instance.getCurrentState().getStateName();

            // Skip if already in target state or past it
            if (currentState.equals(targetState)) {
                logger.info("Order {} already in state {} - skipping", orderId, targetState);
                return;
            }

            // Skip if in terminal state
            if (instance.isCompleted()) {
                logger.info("Order {} workflow already completed - skipping transition to {}", 
                        orderId, targetState);
                return;
            }

            // Execute in separate transaction to prevent rollback propagation
            transitionExecutor.executeTransition(ENTITY_TYPE, orderId, targetState, 
                    actor, actorRole, comment);
            
            logger.info("Order {} automatically transitioned to {}", orderId, targetState);

        } catch (Exception e) {
            logger.warn("Auto-transition to {} failed for order {}: {} - may be invalid transition", 
                    targetState, orderId, e.getMessage());
            // Don't throw - auto-transitions are best-effort
        }
    }
}
