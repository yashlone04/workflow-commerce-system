package com.example.workflowcommerce.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.workflowcommerce.dto.workflow.WorkflowInstanceDTO;
import com.example.workflowcommerce.dto.workflow.WorkflowLogDTO;
import com.example.workflowcommerce.dto.workflow.WorkflowTransitionDTO;
import com.example.workflowcommerce.exception.ResourceNotFoundException;
import com.example.workflowcommerce.exception.WorkflowException;
import com.example.workflowcommerce.model.Order;
import com.example.workflowcommerce.model.OrderItem;
import com.example.workflowcommerce.model.workflow.WorkflowInstance;
import com.example.workflowcommerce.repository.OrderRepository;
import com.example.workflowcommerce.service.workflow.WorkflowEngineService;

/**
 * Service for managing order workflow operations
 * Bridges the Order module with the Workflow Engine
 */
@Service
@Transactional
public class OrderWorkflowService {

    private static final String WORKFLOW_NAME = "OrderLifecycleWorkflow";
    private static final String ENTITY_TYPE = "ORDER";

    @Autowired
    private WorkflowEngineService workflowEngineService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    /**
     * Initialize workflow for a newly created order
     */
    public WorkflowInstance initializeOrderWorkflow(Order order, String createdBy) {
        // Create workflow instance
        WorkflowInstance instance = workflowEngineService.createWorkflowInstance(
                WORKFLOW_NAME, ENTITY_TYPE, order.getOrderId(), createdBy);

        // Link workflow instance to order
        order.setWorkflowInstanceId(instance.getId());
        order.setOrderStatus(instance.getCurrentState().getStateName());
        orderRepository.save(order);

        return instance;
    }

    /**
     * Get workflow instance for an order
     */
    public WorkflowInstanceDTO getOrderWorkflowInstance(Long orderId, String userRole) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getWorkflowInstanceId() == null) {
            throw new WorkflowException("NO_WORKFLOW", 
                    "No workflow instance found for order: " + orderId);
        }

        return workflowEngineService.getWorkflowInstance(order.getWorkflowInstanceId(), userRole);
    }

    /**
     * Get allowed transitions for an order
     */
    public List<WorkflowTransitionDTO> getOrderAllowedTransitions(Long orderId, String userRole) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getWorkflowInstanceId() == null) {
            throw new WorkflowException("NO_WORKFLOW", 
                    "No workflow instance found for order: " + orderId);
        }

        return workflowEngineService.getAllowedTransitions(order.getWorkflowInstanceId(), userRole);
    }

    /**
     * Execute a workflow transition for an order
     */
    public WorkflowInstanceDTO transitionOrder(Long orderId, String targetState, 
                                                String actor, String actorRole, String comment) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getWorkflowInstanceId() == null) {
            throw new WorkflowException("NO_WORKFLOW", 
                    "No workflow instance found for order: " + orderId);
        }

        // Execute transition in workflow engine
        WorkflowInstanceDTO result = workflowEngineService.transition(
                order.getWorkflowInstanceId(), targetState, actor, actorRole, comment);

        // Update order status to match workflow state
        order.setOrderStatus(result.getCurrentState().getStateName());
        
        // Handle specific state transitions
        handleStateTransition(order, targetState);
        
        orderRepository.save(order);

        return result;
    }

    /**
     * Handle business logic for specific state transitions
     */
    private void handleStateTransition(Order order, String targetState) {
        switch (targetState) {
            case "CANCELLED":
            case "REFUNDED":
                // Restore inventory for cancelled/refunded orders
                order.setStatus(false);
                if (order.getOrderItems() != null) {
                    for (OrderItem item : order.getOrderItems()) {
                        orderService.restoreProductInventory(
                                item.getProduct().getProductId(), 
                                item.getQuantity());
                    }
                }
                break;
            case "DELIVERED":
                // Order completed successfully
                break;
            default:
                // No special handling needed
                break;
        }
    }

    /**
     * Get workflow logs for an order
     */
    public List<WorkflowLogDTO> getOrderWorkflowLogs(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getWorkflowInstanceId() == null) {
            throw new WorkflowException("NO_WORKFLOW", 
                    "No workflow instance found for order: " + orderId);
        }

        return workflowEngineService.getWorkflowLogs(order.getWorkflowInstanceId());
    }

    /**
     * Check if an order has an active workflow
     */
    public boolean hasActiveWorkflow(Long orderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return false;
        }
        return orderOpt.get().getWorkflowInstanceId() != null;
    }

    /**
     * Move order to payment pending (convenience method)
     */
    public WorkflowInstanceDTO initiatePayment(Long orderId, String actor, String actorRole) {
        return transitionOrder(orderId, "PAYMENT_PENDING", actor, actorRole, "Payment initiated");
    }

    /**
     * Confirm payment received (convenience method)
     */
    public WorkflowInstanceDTO confirmPayment(Long orderId, String actor, String actorRole) {
        return transitionOrder(orderId, "PAID", actor, actorRole, "Payment confirmed");
    }

    /**
     * Start processing order (convenience method)
     */
    public WorkflowInstanceDTO startProcessing(Long orderId, String actor, String actorRole) {
        return transitionOrder(orderId, "PROCESSING", actor, actorRole, "Order processing started");
    }

    /**
     * Ship order (convenience method)
     */
    public WorkflowInstanceDTO shipOrder(Long orderId, String trackingNumber, String actor, String actorRole) {
        return transitionOrder(orderId, "SHIPPED", actor, actorRole, "Shipped with tracking: " + trackingNumber);
    }

    /**
     * Mark order as delivered (convenience method)
     */
    public WorkflowInstanceDTO markDelivered(Long orderId, String actor, String actorRole) {
        return transitionOrder(orderId, "DELIVERED", actor, actorRole, "Order delivered successfully");
    }

    /**
     * Cancel order (convenience method)
     */
    public WorkflowInstanceDTO cancelOrder(Long orderId, String reason, String actor, String actorRole) {
        return transitionOrder(orderId, "CANCELLED", actor, actorRole, "Cancelled: " + reason);
    }

    /**
     * Refund order (convenience method)
     */
    public WorkflowInstanceDTO refundOrder(Long orderId, String reason, String actor, String actorRole) {
        return transitionOrder(orderId, "REFUNDED", actor, actorRole, "Refunded: " + reason);
    }
}
