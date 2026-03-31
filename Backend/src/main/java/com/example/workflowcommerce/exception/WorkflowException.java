package com.example.workflowcommerce.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Base exception for all workflow-related errors
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class WorkflowException extends RuntimeException {
    
    private final String errorCode;
    
    public WorkflowException(String message) {
        super(message);
        this.errorCode = "WORKFLOW_ERROR";
    }
    
    public WorkflowException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public WorkflowException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "WORKFLOW_ERROR";
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
