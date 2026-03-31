package com.example.workflowcommerce.service.workflow;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.workflowcommerce.exception.BusinessRuleViolationException;
import com.example.workflowcommerce.model.Order;
import com.example.workflowcommerce.model.Payment;
import com.example.workflowcommerce.model.Shipping;
import com.example.workflowcommerce.repository.OrderRepository;
import com.example.workflowcommerce.repository.PaymentRepository;
import com.example.workflowcommerce.repository.ShippingRepository;

/**
 * Validates business rules before allowing workflow state transitions.
 * Ensures data integrity - e.g., cannot mark as PAID without actual payment.
 */
@Service
public class OrderWorkflowRuleValidator {

    private static final Logger logger = LoggerFactory.getLogger(OrderWorkflowRuleValidator.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ShippingRepository shippingRepository;

    /**
     * Validate business rules before allowing a state transition.
     * 
     * @param entityType The type of entity (ORDER, etc.)
     * @param entityId The entity ID
     * @param currentState Current workflow state
     * @param targetState Target workflow state
     * @throws BusinessRuleViolationException if business rules are not met
     */
    public void validateTransition(String entityType, Long entityId, 
                                    String currentState, String targetState) {
        if (!"ORDER".equalsIgnoreCase(entityType)) {
            return; // Only validate ORDER entities for now
        }

        logger.info("Validating business rules for ORDER {} transition: {} -> {}", 
                entityId, currentState, targetState);

        switch (targetState.toUpperCase()) {
            case "PAID":
                validatePaymentExists(entityId);
                break;
            case "PROCESSING":
                validateOrderIsPaid(entityId);
                break;
            case "SHIPPED":
                validateReadyForShipping(entityId);
                break;
            case "DELIVERED":
                validateShippingExists(entityId);
                break;
            case "REFUNDED":
                validateCanRefund(entityId, currentState);
                break;
            default:
                // No special validation needed for other states
                break;
        }
    }

    /**
     * Cannot mark as PAID unless a completed payment exists
     */
    private void validatePaymentExists(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "ORDER_NOT_FOUND", "PAID", 
                        "Order not found: " + orderId));

        Optional<Payment> paymentOpt = paymentRepository.findByOrder(order);
        
        if (paymentOpt.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "NO_PAYMENT_RECORD", "PAID",
                    "Cannot mark order as PAID: No payment record exists. " +
                    "Customer must complete payment first.");
        }

        Payment payment = paymentOpt.get();
        String status = payment.getPaymentStatus();
        
        if (!isPaymentCompleted(status)) {
            throw new BusinessRuleViolationException(
                    "PAYMENT_NOT_COMPLETED", "PAID",
                    "Cannot mark order as PAID: Payment status is '" + status + "'. " +
                    "Payment must be COMPLETED first.");
        }

        logger.info("Payment validation passed for order {}", orderId);
    }

    /**
     * Cannot start processing unless order is paid
     */
    private void validateOrderIsPaid(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "ORDER_NOT_FOUND", "PROCESSING", 
                        "Order not found: " + orderId));

        Optional<Payment> paymentOpt = paymentRepository.findByOrder(order);
        
        if (paymentOpt.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "NO_PAYMENT_FOR_PROCESSING", "PROCESSING",
                    "Cannot start processing: No payment record exists.");
        }

        Payment payment = paymentOpt.get();
        String status = payment.getPaymentStatus();
        
        if (!isPaymentCompleted(status)) {
            throw new BusinessRuleViolationException(
                    "PAYMENT_NOT_COMPLETED", "PROCESSING",
                    "Cannot start processing: Payment is not completed (status: " + status + ")");
        }
    }

    /**
     * Cannot ship unless order is paid and shipping record exists.
     * The workflow transition to SHIPPED happens AFTER the shipping record is committed
     * (via @TransactionalEventListener with AFTER_COMMIT phase).
     */
    private void validateReadyForShipping(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "ORDER_NOT_FOUND", "SHIPPED", 
                        "Order not found: " + orderId));

        // Check payment exists and is completed
        Optional<Payment> paymentOpt = paymentRepository.findByOrder(order);
        if (paymentOpt.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "NO_PAYMENT_FOR_SHIPPING", "SHIPPED",
                    "Cannot ship order: No payment record exists.");
        }

        Payment payment = paymentOpt.get();
        if (!isPaymentCompleted(payment.getPaymentStatus())) {
            throw new BusinessRuleViolationException(
                    "PAYMENT_NOT_COMPLETED", "SHIPPED",
                    "Cannot ship order: Payment not completed.");
        }

        // Check shipping record exists (created by ShippingService before this validation runs)
        if (!shippingRepository.existsByOrderOrderId(orderId)) {
            throw new BusinessRuleViolationException(
                    "NO_SHIPPING_RECORD", "SHIPPED",
                    "Cannot mark as SHIPPED: No shipping record exists. " +
                    "Admin must create shipping first.");
        }

        logger.info("Shipping validation passed for order {}", orderId);
    }

    /**
     * Cannot mark as delivered unless shipping record exists and status is appropriate.
     * Since ShippingService enforces Shipped -> In Transit -> Delivered,
     * when this is called the shipping should already be updated to Delivered.
     */
    private void validateShippingExists(Long orderId) {
        if (!shippingRepository.existsByOrderOrderId(orderId)) {
            throw new BusinessRuleViolationException(
                    "NO_SHIPPING_RECORD", "DELIVERED",
                    "Cannot mark as DELIVERED: No shipping record exists.");
        }

        Optional<Shipping> shippingOpt = shippingRepository.findByOrderOrderId(orderId);
        if (shippingOpt.isPresent()) {
            Shipping shipping = shippingOpt.get();
            String shippingStatus = shipping.getShippingStatus();
            
            // Shipping must be in Delivered status (set by ShippingService before event fires)
            if (!"Delivered".equalsIgnoreCase(shippingStatus)) {
                throw new BusinessRuleViolationException(
                        "SHIPPING_NOT_DELIVERED", "DELIVERED",
                        "Cannot mark order as DELIVERED: Shipping status is '" + shippingStatus + 
                        "'. Must update shipping to 'Delivered' first.");
            }
            
            logger.info("Delivery validation passed for order {}", orderId);
        }
    }

    /**
     * Validate refund conditions
     */
    private void validateCanRefund(Long orderId, String currentState) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "ORDER_NOT_FOUND", "REFUNDED", 
                        "Order not found: " + orderId));

        // Can only refund if payment exists
        Optional<Payment> paymentOpt = paymentRepository.findByOrder(order);
        if (paymentOpt.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "NO_PAYMENT_TO_REFUND", "REFUNDED",
                    "Cannot refund: No payment record exists to refund.");
        }

        // Optionally: check if already refunded
        Payment payment = paymentOpt.get();
        if ("REFUNDED".equalsIgnoreCase(payment.getPaymentStatus())) {
            throw new BusinessRuleViolationException(
                    "ALREADY_REFUNDED", "REFUNDED",
                    "Cannot refund: Payment has already been refunded.");
        }

        logger.info("Refund validation passed for order {}", orderId);
    }

    /**
     * Check if payment status indicates successful payment.
     * Accepts: COMPLETED, SUCCESS, Paid (for backwards compatibility)
     */
    private boolean isPaymentCompleted(String status) {
        if (status == null) return false;
        return "COMPLETED".equalsIgnoreCase(status) || 
               "SUCCESS".equalsIgnoreCase(status) || 
               "Paid".equalsIgnoreCase(status);
    }
}
