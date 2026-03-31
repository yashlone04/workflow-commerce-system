package com.example.workflowcommerce.dto.workflow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for executing a workflow transition
 */
public class TransitionRequest {
    
    @NotNull(message = "Target state is required")
    @NotBlank(message = "Target state cannot be blank")
    private String targetState;
    
    private String comment;
    
    public TransitionRequest() {}
    
    public TransitionRequest(String targetState, String comment) {
        this.targetState = targetState;
        this.comment = comment;
    }
    
    // Getters and Setters
    public String getTargetState() { return targetState; }
    public void setTargetState(String targetState) { this.targetState = targetState; }
    
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
