package com.example.workflowcommerce.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.workflowcommerce.dto.workflow.TransitionRequest;
import com.example.workflowcommerce.dto.workflow.WorkflowDefinitionDTO;
import com.example.workflowcommerce.dto.workflow.WorkflowInstanceDTO;
import com.example.workflowcommerce.dto.workflow.WorkflowLogDTO;
import com.example.workflowcommerce.dto.workflow.WorkflowStatsDTO;
import com.example.workflowcommerce.dto.workflow.WorkflowTransitionDTO;
import com.example.workflowcommerce.model.Order;
import com.example.workflowcommerce.model.workflow.WorkflowInstance;
import com.example.workflowcommerce.repository.OrderRepository;
import com.example.workflowcommerce.repository.workflow.WorkflowInstanceRepository;
import com.example.workflowcommerce.security.services.UserDetailsImpl;
import com.example.workflowcommerce.service.workflow.WorkflowEngineService;

import jakarta.validation.Valid;

/**
 * REST Controller for Workflow Engine Operations
 * Provides APIs for workflow management, transitions, and audit logs
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/workflow")
public class WorkflowController {

    @Autowired
    private WorkflowEngineService workflowEngineService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private WorkflowInstanceRepository workflowInstanceRepository;

    // ==================== WORKFLOW DEFINITIONS ====================

    /**
     * Get all active workflow definitions
     */
    @GetMapping("/definitions")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<WorkflowDefinitionDTO>> getAllWorkflowDefinitions() {
        return ResponseEntity.ok(workflowEngineService.getAllWorkflowDefinitions());
    }

    /**
     * Get workflow definition by name with full details
     */
    @GetMapping("/definitions/{name}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<WorkflowDefinitionDTO> getWorkflowDefinition(@PathVariable String name) {
        return ResponseEntity.ok(workflowEngineService.getWorkflowDefinition(name));
    }

    /**
     * Get workflow statistics
     */
    @GetMapping("/definitions/{id}/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WorkflowStatsDTO> getWorkflowStats(@PathVariable Long id) {
        return ResponseEntity.ok(workflowEngineService.getWorkflowStats(id));
    }

    // ==================== WORKFLOW INSTANCES ====================

    /**
     * Get all active workflow instances
     */
    @GetMapping("/instances")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<WorkflowInstanceDTO>> getAllActiveInstances(Authentication authentication) {
        String role = getPrimaryRole(authentication);
        return ResponseEntity.ok(workflowEngineService.getAllActiveInstances(role));
    }

    /**
     * Get workflow instance by ID
     */
    @GetMapping("/instances/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<WorkflowInstanceDTO> getWorkflowInstance(
            @PathVariable Long id, Authentication authentication) {
        String role = getPrimaryRole(authentication);
        return ResponseEntity.ok(workflowEngineService.getWorkflowInstance(id, role));
    }

    /**
     * Get active workflow instance for an entity
     */
    @GetMapping("/instances/entity/{entityType}/{entityId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<WorkflowInstanceDTO> getWorkflowInstanceByEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            Authentication authentication) {
        String role = getPrimaryRole(authentication);
        return ResponseEntity.ok(workflowEngineService.getActiveWorkflowInstance(entityType, entityId, role));
    }

    /**
     * Get all instances by entity type
     */
    @GetMapping("/instances/type/{entityType}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<WorkflowInstanceDTO>> getInstancesByEntityType(
            @PathVariable String entityType, Authentication authentication) {
        String role = getPrimaryRole(authentication);
        return ResponseEntity.ok(workflowEngineService.getInstancesByEntityType(entityType, role));
    }

    /**
     * Create a new workflow instance
     */
    @PostMapping("/instances")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WorkflowInstanceDTO> createWorkflowInstance(
            @RequestParam String workflowName,
            @RequestParam String entityType,
            @RequestParam Long entityId,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String role = getPrimaryRole(authentication);
        
        WorkflowInstance instance = workflowEngineService.createWorkflowInstance(
                workflowName, entityType, entityId, userDetails.getUsername());
        
        return ResponseEntity.ok(workflowEngineService.getWorkflowInstance(instance.getId(), role));
    }

    // ==================== TRANSITIONS ====================

    /**
     * Get allowed transitions for a workflow instance
     */
    @GetMapping("/instances/{id}/transitions")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<WorkflowTransitionDTO>> getAllowedTransitions(
            @PathVariable Long id, Authentication authentication) {
        String role = getPrimaryRole(authentication);
        return ResponseEntity.ok(workflowEngineService.getAllowedTransitions(id, role));
    }

    /**
     * Execute a workflow transition
     */
    @PostMapping("/instances/{id}/transition")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<WorkflowInstanceDTO> executeTransition(
            @PathVariable Long id,
            @Valid @RequestBody TransitionRequest request,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String role = getPrimaryRole(authentication);
        
        WorkflowInstanceDTO result = workflowEngineService.transition(
                id, 
                request.getTargetState(), 
                userDetails.getUsername(), 
                role,
                request.getComment()
        );
        
        return ResponseEntity.ok(result);
    }

    /**
     * Execute a workflow transition by entity
     */
    @PostMapping("/instances/entity/{entityType}/{entityId}/transition")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<WorkflowInstanceDTO> executeTransitionByEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @Valid @RequestBody TransitionRequest request,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String role = getPrimaryRole(authentication);
        
        WorkflowInstanceDTO result = workflowEngineService.transitionByEntity(
                entityType, 
                entityId,
                request.getTargetState(), 
                userDetails.getUsername(), 
                role,
                request.getComment()
        );
        
        return ResponseEntity.ok(result);
    }

    // ==================== AUDIT LOGS ====================

    /**
     * Get workflow logs for an instance
     */
    @GetMapping("/instances/{id}/logs")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<WorkflowLogDTO>> getWorkflowLogs(@PathVariable Long id) {
        return ResponseEntity.ok(workflowEngineService.getWorkflowLogs(id));
    }

    /**
     * Get workflow logs for an entity
     */
    @GetMapping("/logs/entity/{entityType}/{entityId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<WorkflowLogDTO>> getWorkflowLogsByEntity(
            @PathVariable String entityType, @PathVariable Long entityId) {
        return ResponseEntity.ok(workflowEngineService.getWorkflowLogsByEntity(entityType, entityId));
    }

    /**
     * Get recent logs (last 24 hours)
     */
    @GetMapping("/logs/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<WorkflowLogDTO>> getRecentLogs() {
        return ResponseEntity.ok(workflowEngineService.getRecentLogs());
    }

    // ==================== MIGRATION / ADMIN TOOLS ====================

    /**
     * Migrate all orders without workflow instances
     * Creates workflow instances for all orders that don't have them
     */
    @PostMapping("/migrate/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> migrateOrdersToWorkflow(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        List<Order> allOrders = orderRepository.findAll();
        List<Long> migratedOrderIds = new ArrayList<>();
        List<Long> failedOrderIds = new ArrayList<>();
        
        for (Order order : allOrders) {
            // Check if workflow already exists (active = not completed)
            boolean hasWorkflow = workflowInstanceRepository
                    .findByEntityTypeAndEntityIdAndIsCompletedFalse("ORDER", order.getOrderId())
                    .isPresent();
            
            if (!hasWorkflow) {
                try {
                    workflowEngineService.createWorkflowInstance(
                            "OrderLifecycleWorkflow", 
                            "ORDER", 
                            order.getOrderId(), 
                            userDetails.getUsername()
                    );
                    migratedOrderIds.add(order.getOrderId());
                } catch (Exception e) {
                    failedOrderIds.add(order.getOrderId());
                }
            }
        }
        
        return ResponseEntity.ok(java.util.Map.of(
                "message", "Migration complete",
                "migratedOrderIds", migratedOrderIds,
                "failedOrderIds", failedOrderIds,
                "totalMigrated", migratedOrderIds.size(),
                "totalFailed", failedOrderIds.size()
        ));
    }

    // ==================== HELPER METHODS ====================

    private String getPrimaryRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(role -> role.startsWith("ROLE_"))
                .findFirst()
                .orElse("ROLE_USER");
    }
}
