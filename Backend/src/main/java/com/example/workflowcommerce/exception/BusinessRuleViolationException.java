package com.example.workflowcommerce.exception;

/**
 * Exception thrown when a workflow transition violates business rules.
 * For example: trying to mark order as PAID without actual payment.
 */
public class BusinessRuleViolationException extends RuntimeException {
    
    private final String ruleCode;
    private final String targetState;

    public BusinessRuleViolationException(String ruleCode, String targetState, String message) {
        super(message);
        this.ruleCode = ruleCode;
        this.targetState = targetState;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public String getTargetState() {
        return targetState;
    }
}
