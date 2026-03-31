package com.example.workflowcommerce.model.workflow;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Represents an active workflow instance for a specific entity.
 * Links a domain entity (e.g., Order) to its current workflow state.
 */
@Entity
@Table(name = "workflow_instances", indexes = {
    @Index(name = "idx_instance_workflow", columnList = "workflow_id"),
    @Index(name = "idx_instance_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_instance_state", columnList = "current_state_id"),
    @Index(name = "idx_instance_created", columnList = "created_at")
})
public class WorkflowInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private WorkflowDefinition workflow;

    @NotBlank
    @Size(max = 50)
    @Column(name = "entity_type", nullable = false)
    private String entityType; // e.g., "ORDER", "PAYMENT"

    @Column(name = "entity_id", nullable = false)
    private Long entityId; // ID of the related entity (e.g., order_id)

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "current_state_id", nullable = false)
    private WorkflowState currentState;

    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted = false;

    @Column(name = "created_by")
    @Size(max = 50)
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "workflowInstance", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("timestamp DESC")
    private List<WorkflowLog> logs = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public WorkflowInstance() {}

    public WorkflowInstance(WorkflowDefinition workflow, String entityType, Long entityId, 
                            WorkflowState initialState, String createdBy) {
        this.workflow = workflow;
        this.entityType = entityType;
        this.entityId = entityId;
        this.currentState = initialState;
        this.createdBy = createdBy;
        this.isCompleted = false;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public WorkflowDefinition getWorkflow() { return workflow; }
    public void setWorkflow(WorkflowDefinition workflow) { this.workflow = workflow; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public WorkflowState getCurrentState() { return currentState; }
    public void setCurrentState(WorkflowState currentState) { this.currentState = currentState; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public List<WorkflowLog> getLogs() { return logs; }
    public void setLogs(List<WorkflowLog> logs) { this.logs = logs; }

    /**
     * Mark this workflow instance as completed
     */
    public void complete() {
        this.isCompleted = true;
        this.completedAt = LocalDateTime.now();
    }
}
