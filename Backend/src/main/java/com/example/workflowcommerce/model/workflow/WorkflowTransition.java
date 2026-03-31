package com.example.workflowcommerce.model.workflow;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Represents a valid transition between two states in a workflow.
 * Transitions define the allowed state changes and which roles can perform them.
 */
@Entity
@Table(name = "workflow_transitions", indexes = {
    @Index(name = "idx_transition_workflow", columnList = "workflow_id"),
    @Index(name = "idx_transition_from_state", columnList = "from_state_id"),
    @Index(name = "idx_transition_to_state", columnList = "to_state_id")
})
public class WorkflowTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private WorkflowDefinition workflow;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "from_state_id", nullable = false)
    private WorkflowState fromState;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "to_state_id", nullable = false)
    private WorkflowState toState;

    @NotBlank
    @Size(max = 100)
    @Column(name = "allowed_roles", nullable = false)
    private String allowedRoles; // Comma-separated roles: "ROLE_ADMIN,ROLE_USER,SYSTEM"

    @Size(max = 200)
    @Column(name = "action_name")
    private String actionName; // Human-readable action name, e.g., "Mark as Paid"

    @Size(max = 500)
    private String description;

    @Column(name = "requires_comment", nullable = false)
    private boolean requiresComment = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructors
    public WorkflowTransition() {}

    public WorkflowTransition(WorkflowDefinition workflow, WorkflowState fromState, 
                              WorkflowState toState, String allowedRoles, String actionName) {
        this.workflow = workflow;
        this.fromState = fromState;
        this.toState = toState;
        this.allowedRoles = allowedRoles;
        this.actionName = actionName;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public WorkflowDefinition getWorkflow() { return workflow; }
    public void setWorkflow(WorkflowDefinition workflow) { this.workflow = workflow; }

    public WorkflowState getFromState() { return fromState; }
    public void setFromState(WorkflowState fromState) { this.fromState = fromState; }

    public WorkflowState getToState() { return toState; }
    public void setToState(WorkflowState toState) { this.toState = toState; }

    public String getAllowedRoles() { return allowedRoles; }
    public void setAllowedRoles(String allowedRoles) { this.allowedRoles = allowedRoles; }

    public String getActionName() { return actionName; }
    public void setActionName(String actionName) { this.actionName = actionName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isRequiresComment() { return requiresComment; }
    public void setRequiresComment(boolean requiresComment) { this.requiresComment = requiresComment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /**
     * Check if a given role is allowed to perform this transition
     */
    public boolean isRoleAllowed(String role) {
        if (allowedRoles == null || allowedRoles.isEmpty()) {
            return false;
        }
        String[] roles = allowedRoles.split(",");
        for (String allowedRole : roles) {
            if (allowedRole.trim().equalsIgnoreCase(role) || 
                allowedRole.trim().equalsIgnoreCase("SYSTEM")) {
                return true;
            }
        }
        return false;
    }
}
