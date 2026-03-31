package com.example.workflowcommerce.dto.workflow;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for workflow instance with full details
 */
public class WorkflowInstanceDTO {
    
    private Long id;
    private Long workflowId;
    private String workflowName;
    private String entityType;
    private Long entityId;
    private WorkflowStateDTO currentState;
    private boolean isCompleted;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private List<WorkflowTransitionDTO> allowedTransitions;
    private List<WorkflowLogDTO> recentLogs;
    
    public WorkflowInstanceDTO() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getWorkflowId() { return workflowId; }
    public void setWorkflowId(Long workflowId) { this.workflowId = workflowId; }
    
    public String getWorkflowName() { return workflowName; }
    public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }
    
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    
    public WorkflowStateDTO getCurrentState() { return currentState; }
    public void setCurrentState(WorkflowStateDTO currentState) { this.currentState = currentState; }
    
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
    
    public List<WorkflowTransitionDTO> getAllowedTransitions() { return allowedTransitions; }
    public void setAllowedTransitions(List<WorkflowTransitionDTO> allowedTransitions) { 
        this.allowedTransitions = allowedTransitions; 
    }
    
    public List<WorkflowLogDTO> getRecentLogs() { return recentLogs; }
    public void setRecentLogs(List<WorkflowLogDTO> recentLogs) { this.recentLogs = recentLogs; }
}
