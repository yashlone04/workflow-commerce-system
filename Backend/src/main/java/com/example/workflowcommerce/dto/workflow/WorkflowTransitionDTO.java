package com.example.workflowcommerce.dto.workflow;

/**
 * DTO for available workflow transitions
 */
public class WorkflowTransitionDTO {
    
    private Long id;
    private String fromStateName;
    private String toStateName;
    private String actionName;
    private String description;
    private String allowedRoles;
    private boolean requiresComment;
    private String toStateColorCode;
    
    public WorkflowTransitionDTO() {}
    
    public WorkflowTransitionDTO(Long id, String fromStateName, String toStateName, 
                                  String actionName, String description, String allowedRoles,
                                  boolean requiresComment, String toStateColorCode) {
        this.id = id;
        this.fromStateName = fromStateName;
        this.toStateName = toStateName;
        this.actionName = actionName;
        this.description = description;
        this.allowedRoles = allowedRoles;
        this.requiresComment = requiresComment;
        this.toStateColorCode = toStateColorCode;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFromStateName() { return fromStateName; }
    public void setFromStateName(String fromStateName) { this.fromStateName = fromStateName; }
    
    public String getToStateName() { return toStateName; }
    public void setToStateName(String toStateName) { this.toStateName = toStateName; }
    
    public String getActionName() { return actionName; }
    public void setActionName(String actionName) { this.actionName = actionName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getAllowedRoles() { return allowedRoles; }
    public void setAllowedRoles(String allowedRoles) { this.allowedRoles = allowedRoles; }
    
    public boolean isRequiresComment() { return requiresComment; }
    public void setRequiresComment(boolean requiresComment) { this.requiresComment = requiresComment; }
    
    public String getToStateColorCode() { return toStateColorCode; }
    public void setToStateColorCode(String toStateColorCode) { this.toStateColorCode = toStateColorCode; }
}
