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
import jakarta.validation.constraints.Size;

/**
 * Represents a log entry for workflow state transitions.
 * Provides complete audit trail for all workflow operations.
 */
@Entity
@Table(name = "workflow_logs", indexes = {
    @Index(name = "idx_log_instance", columnList = "workflow_instance_id"),
    @Index(name = "idx_log_timestamp", columnList = "timestamp"),
    @Index(name = "idx_log_actor", columnList = "performed_by")
})
public class WorkflowLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_instance_id", nullable = false)
    private WorkflowInstance workflowInstance;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "from_state_id")
    private WorkflowState fromState;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "to_state_id", nullable = false)
    private WorkflowState toState;

    @Size(max = 50)
    @Column(name = "performed_by", nullable = false)
    private String performedBy; // Username or "SYSTEM"

    @Size(max = 50)
    @Column(name = "performed_role")
    private String performedRole; // Role used for the transition

    @Size(max = 500)
    private String comment;

    @Size(max = 50)
    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "action_name")
    @Size(max = 100)
    private String actionName; // Human-readable action performed

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    // Constructors
    public WorkflowLog() {}

    public WorkflowLog(WorkflowInstance workflowInstance, WorkflowState fromState, 
                       WorkflowState toState, String performedBy, String performedRole,
                       String actionName, String comment) {
        this.workflowInstance = workflowInstance;
        this.fromState = fromState;
        this.toState = toState;
        this.performedBy = performedBy;
        this.performedRole = performedRole;
        this.actionName = actionName;
        this.comment = comment;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public WorkflowInstance getWorkflowInstance() { return workflowInstance; }
    public void setWorkflowInstance(WorkflowInstance workflowInstance) { this.workflowInstance = workflowInstance; }

    public WorkflowState getFromState() { return fromState; }
    public void setFromState(WorkflowState fromState) { this.fromState = fromState; }

    public WorkflowState getToState() { return toState; }
    public void setToState(WorkflowState toState) { this.toState = toState; }

    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }

    public String getPerformedRole() { return performedRole; }
    public void setPerformedRole(String performedRole) { this.performedRole = performedRole; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getActionName() { return actionName; }
    public void setActionName(String actionName) { this.actionName = actionName; }
}
