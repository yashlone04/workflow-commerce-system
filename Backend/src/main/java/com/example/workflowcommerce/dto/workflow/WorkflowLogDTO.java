package com.example.workflowcommerce.dto.workflow;

import java.time.LocalDateTime;

/**
 * DTO for workflow log entries
 */
public class WorkflowLogDTO {
    
    private Long id;
    private Long workflowInstanceId;
    private String entityType;
    private Long entityId;
    private String fromStateName;
    private String toStateName;
    private String fromStateColorCode;
    private String toStateColorCode;
    private String performedBy;
    private String performedRole;
    private String actionName;
    private String comment;
    private LocalDateTime timestamp;
    
    public WorkflowLogDTO() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getWorkflowInstanceId() { return workflowInstanceId; }
    public void setWorkflowInstanceId(Long workflowInstanceId) { this.workflowInstanceId = workflowInstanceId; }
    
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    
    public String getFromStateName() { return fromStateName; }
    public void setFromStateName(String fromStateName) { this.fromStateName = fromStateName; }
    
    public String getToStateName() { return toStateName; }
    public void setToStateName(String toStateName) { this.toStateName = toStateName; }
    
    public String getFromStateColorCode() { return fromStateColorCode; }
    public void setFromStateColorCode(String fromStateColorCode) { this.fromStateColorCode = fromStateColorCode; }
    
    public String getToStateColorCode() { return toStateColorCode; }
    public void setToStateColorCode(String toStateColorCode) { this.toStateColorCode = toStateColorCode; }
    
    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }
    
    public String getPerformedRole() { return performedRole; }
    public void setPerformedRole(String performedRole) { this.performedRole = performedRole; }
    
    public String getActionName() { return actionName; }
    public void setActionName(String actionName) { this.actionName = actionName; }
    
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
