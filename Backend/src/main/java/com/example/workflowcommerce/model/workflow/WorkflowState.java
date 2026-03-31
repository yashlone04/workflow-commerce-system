package com.example.workflowcommerce.model.workflow;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Represents a state within a workflow.
 * States can be initial (entry point), terminal (end point), or intermediate.
 */
@Entity
@Table(name = "workflow_states", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"workflow_id", "state_name"})
})
public class WorkflowState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private WorkflowDefinition workflow;

    @NotBlank
    @Size(max = 50)
    @Column(name = "state_name", nullable = false)
    private String stateName;

    @Size(max = 200)
    private String description;

    @Column(name = "is_initial", nullable = false)
    private boolean isInitial = false;

    @Column(name = "is_terminal", nullable = false)
    private boolean isTerminal = false;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Size(max = 7)
    @Column(name = "color_code")
    private String colorCode; // For UI visualization, e.g., "#28a745"

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructors
    public WorkflowState() {}

    public WorkflowState(WorkflowDefinition workflow, String stateName, String description, 
                         boolean isInitial, boolean isTerminal, Integer displayOrder, String colorCode) {
        this.workflow = workflow;
        this.stateName = stateName;
        this.description = description;
        this.isInitial = isInitial;
        this.isTerminal = isTerminal;
        this.displayOrder = displayOrder;
        this.colorCode = colorCode;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public WorkflowDefinition getWorkflow() { return workflow; }
    public void setWorkflow(WorkflowDefinition workflow) { this.workflow = workflow; }

    public String getStateName() { return stateName; }
    public void setStateName(String stateName) { this.stateName = stateName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isInitial() { return isInitial; }
    public void setInitial(boolean initial) { isInitial = initial; }

    public boolean isTerminal() { return isTerminal; }
    public void setTerminal(boolean terminal) { isTerminal = terminal; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public String getColorCode() { return colorCode; }
    public void setColorCode(String colorCode) { this.colorCode = colorCode; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
