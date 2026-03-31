package com.example.workflowcommerce.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.workflowcommerce.model.workflow.WorkflowDefinition;
import com.example.workflowcommerce.model.workflow.WorkflowState;
import com.example.workflowcommerce.model.workflow.WorkflowTransition;
import com.example.workflowcommerce.repository.workflow.WorkflowDefinitionRepository;
import com.example.workflowcommerce.repository.workflow.WorkflowStateRepository;
import com.example.workflowcommerce.repository.workflow.WorkflowTransitionRepository;

/**
 * Initializes the Order Lifecycle Workflow
 * This creates the states and transitions for order processing
 */
@Component
@Order(2) // Run after DataInitializer
public class WorkflowDataInitializer implements CommandLineRunner {

    @Autowired
    private WorkflowDefinitionRepository workflowDefinitionRepository;

    @Autowired
    private WorkflowStateRepository workflowStateRepository;

    @Autowired
    private WorkflowTransitionRepository workflowTransitionRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        initializeOrderWorkflow();
        initializePaymentWorkflow();
        initializeShipmentWorkflow();
    }

    private void initializeOrderWorkflow() {
        // Check if already exists
        if (workflowDefinitionRepository.existsByName("OrderLifecycleWorkflow")) {
            return;
        }

        // Create workflow definition
        WorkflowDefinition workflow = new WorkflowDefinition(
                "OrderLifecycleWorkflow",
                "Manages the complete lifecycle of customer orders from creation to delivery or cancellation",
                "ORDER"
        );
        workflow = workflowDefinitionRepository.save(workflow);

        // Create states
        Map<String, WorkflowState> states = new HashMap<>();

        states.put("CREATED", createState(workflow, "CREATED", 
                "Order has been created and is awaiting payment", 
                true, false, 1, "#6c757d"));

        states.put("PAYMENT_PENDING", createState(workflow, "PAYMENT_PENDING", 
                "Waiting for payment confirmation", 
                false, false, 2, "#ffc107"));

        states.put("PAID", createState(workflow, "PAID", 
                "Payment has been received and confirmed", 
                false, false, 3, "#17a2b8"));

        states.put("PROCESSING", createState(workflow, "PROCESSING", 
                "Order is being processed and prepared", 
                false, false, 4, "#007bff"));

        states.put("SHIPPED", createState(workflow, "SHIPPED", 
                "Order has been shipped to customer", 
                false, false, 5, "#6610f2"));

        states.put("DELIVERED", createState(workflow, "DELIVERED", 
                "Order has been delivered successfully", 
                false, true, 6, "#28a745"));

        states.put("CANCELLED", createState(workflow, "CANCELLED", 
                "Order has been cancelled", 
                false, true, 7, "#dc3545"));

        states.put("REFUNDED", createState(workflow, "REFUNDED", 
                "Order has been refunded", 
                false, true, 8, "#e83e8c"));

        // Create transitions
        // CREATED transitions
        createTransition(workflow, states.get("CREATED"), states.get("PAYMENT_PENDING"),
                "ROLE_USER,ROLE_ADMIN,SYSTEM", "Initiate Payment", 
                "Move order to payment pending state", false);

        createTransition(workflow, states.get("CREATED"), states.get("CANCELLED"),
                "ROLE_USER,ROLE_ADMIN", "Cancel Order", 
                "Cancel the order before payment", false);

        // PAYMENT_PENDING transitions
        createTransition(workflow, states.get("PAYMENT_PENDING"), states.get("PAID"),
                "SYSTEM,ROLE_ADMIN", "Confirm Payment", 
                "Payment has been verified and confirmed", false);

        createTransition(workflow, states.get("PAYMENT_PENDING"), states.get("CANCELLED"),
                "ROLE_USER,ROLE_ADMIN", "Cancel Order", 
                "Cancel order due to payment failure or customer request", true);

        // PAID transitions
        createTransition(workflow, states.get("PAID"), states.get("PROCESSING"),
                "ROLE_ADMIN,SYSTEM", "Start Processing", 
                "Begin order processing and preparation", false);

        createTransition(workflow, states.get("PAID"), states.get("REFUNDED"),
                "ROLE_ADMIN", "Refund Order", 
                "Issue full refund for the order", true);

        // PROCESSING transitions
        createTransition(workflow, states.get("PROCESSING"), states.get("SHIPPED"),
                "ROLE_ADMIN", "Ship Order", 
                "Order has been shipped to carrier", false);

        createTransition(workflow, states.get("PROCESSING"), states.get("REFUNDED"),
                "ROLE_ADMIN", "Refund Order", 
                "Issue refund during processing", true);

        // SHIPPED transitions
        createTransition(workflow, states.get("SHIPPED"), states.get("DELIVERED"),
                "ROLE_ADMIN,SYSTEM", "Mark Delivered", 
                "Confirm order delivery to customer", false);

        createTransition(workflow, states.get("SHIPPED"), states.get("REFUNDED"),
                "ROLE_ADMIN", "Refund Order", 
                "Issue refund for shipped order", true);

        System.out.println("✅ OrderLifecycleWorkflow initialized with " + states.size() + " states");
    }

    private void initializePaymentWorkflow() {
        if (workflowDefinitionRepository.existsByName("PaymentWorkflow")) {
            return;
        }

        WorkflowDefinition workflow = new WorkflowDefinition(
                "PaymentWorkflow",
                "Tracks payment processing status",
                "PAYMENT"
        );
        workflow = workflowDefinitionRepository.save(workflow);

        Map<String, WorkflowState> states = new HashMap<>();

        states.put("INITIATED", createState(workflow, "INITIATED", 
                "Payment has been initiated", true, false, 1, "#6c757d"));

        states.put("PROCESSING", createState(workflow, "PROCESSING", 
                "Payment is being processed", false, false, 2, "#ffc107"));

        states.put("COMPLETED", createState(workflow, "COMPLETED", 
                "Payment completed successfully", false, true, 3, "#28a745"));

        states.put("FAILED", createState(workflow, "FAILED", 
                "Payment failed", false, true, 4, "#dc3545"));

        states.put("REFUNDED", createState(workflow, "REFUNDED", 
                "Payment has been refunded", false, true, 5, "#e83e8c"));

        // Transitions
        createTransition(workflow, states.get("INITIATED"), states.get("PROCESSING"),
                "SYSTEM", "Process Payment", "Begin payment processing", false);

        createTransition(workflow, states.get("PROCESSING"), states.get("COMPLETED"),
                "SYSTEM", "Complete Payment", "Payment confirmed", false);

        createTransition(workflow, states.get("PROCESSING"), states.get("FAILED"),
                "SYSTEM", "Payment Failed", "Payment processing failed", false);

        createTransition(workflow, states.get("COMPLETED"), states.get("REFUNDED"),
                "ROLE_ADMIN", "Refund Payment", "Issue refund", true);

        System.out.println("✅ PaymentWorkflow initialized with " + states.size() + " states");
    }

    private void initializeShipmentWorkflow() {
        if (workflowDefinitionRepository.existsByName("ShipmentWorkflow")) {
            return;
        }

        WorkflowDefinition workflow = new WorkflowDefinition(
                "ShipmentWorkflow",
                "Tracks shipment status through delivery process",
                "SHIPMENT"
        );
        workflow = workflowDefinitionRepository.save(workflow);

        Map<String, WorkflowState> states = new HashMap<>();

        states.put("PENDING", createState(workflow, "PENDING", 
                "Shipment pending pickup", true, false, 1, "#6c757d"));

        states.put("PICKED_UP", createState(workflow, "PICKED_UP", 
                "Package picked up by carrier", false, false, 2, "#17a2b8"));

        states.put("IN_TRANSIT", createState(workflow, "IN_TRANSIT", 
                "Package in transit", false, false, 3, "#007bff"));

        states.put("OUT_FOR_DELIVERY", createState(workflow, "OUT_FOR_DELIVERY", 
                "Package out for delivery", false, false, 4, "#6610f2"));

        states.put("DELIVERED", createState(workflow, "DELIVERED", 
                "Package delivered", false, true, 5, "#28a745"));

        states.put("RETURNED", createState(workflow, "RETURNED", 
                "Package returned to sender", false, true, 6, "#dc3545"));

        // Transitions
        createTransition(workflow, states.get("PENDING"), states.get("PICKED_UP"),
                "ROLE_ADMIN,SYSTEM", "Mark Picked Up", "Carrier has picked up package", false);

        createTransition(workflow, states.get("PICKED_UP"), states.get("IN_TRANSIT"),
                "ROLE_ADMIN,SYSTEM", "Mark In Transit", "Package in transit", false);

        createTransition(workflow, states.get("IN_TRANSIT"), states.get("OUT_FOR_DELIVERY"),
                "ROLE_ADMIN,SYSTEM", "Out for Delivery", "Package out for delivery", false);

        createTransition(workflow, states.get("OUT_FOR_DELIVERY"), states.get("DELIVERED"),
                "ROLE_ADMIN,SYSTEM", "Mark Delivered", "Confirm delivery", false);

        createTransition(workflow, states.get("OUT_FOR_DELIVERY"), states.get("RETURNED"),
                "ROLE_ADMIN", "Return Package", "Return to sender", true);

        System.out.println("✅ ShipmentWorkflow initialized with " + states.size() + " states");
    }

    private WorkflowState createState(WorkflowDefinition workflow, String stateName, 
                                       String description, boolean isInitial, boolean isTerminal, 
                                       Integer displayOrder, String colorCode) {
        WorkflowState state = new WorkflowState(workflow, stateName, description, 
                isInitial, isTerminal, displayOrder, colorCode);
        return workflowStateRepository.save(state);
    }

    private void createTransition(WorkflowDefinition workflow, WorkflowState fromState, 
                                   WorkflowState toState, String allowedRoles, String actionName, 
                                   String description, boolean requiresComment) {
        WorkflowTransition transition = new WorkflowTransition(workflow, fromState, toState, 
                allowedRoles, actionName);
        transition.setDescription(description);
        transition.setRequiresComment(requiresComment);
        workflowTransitionRepository.save(transition);
    }
}
