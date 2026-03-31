package com.example.workflowcommerce.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a user does not have permission to perform a workflow action
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedTransitionException extends WorkflowException {
    
    public UnauthorizedTransitionException(String role, String transition) {
        super("UNAUTHORIZED_TRANSITION", 
              String.format("Role '%s' is not authorized to perform transition: %s", role, transition));
    }
    
    public UnauthorizedTransitionException(String message) {
        super("UNAUTHORIZED_TRANSITION", message);
    }
}
