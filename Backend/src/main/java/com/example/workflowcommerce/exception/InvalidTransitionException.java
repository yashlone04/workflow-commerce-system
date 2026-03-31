package com.example.workflowcommerce.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a workflow transition is not valid
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidTransitionException extends WorkflowException {
    
    public InvalidTransitionException(String fromState, String toState) {
        super("INVALID_TRANSITION", 
              String.format("Transition from '%s' to '%s' is not allowed", fromState, toState));
    }
    
    public InvalidTransitionException(String message) {
        super("INVALID_TRANSITION", message);
    }
}
