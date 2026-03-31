package com.example.workflowcommerce.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested resource is not found
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    
    private final String resourceType;
    private final String identifier;
    
    public ResourceNotFoundException(String resourceType, String identifier) {
        super(String.format("%s not found with identifier: %s", resourceType, identifier));
        this.resourceType = resourceType;
        this.identifier = identifier;
    }
    
    public ResourceNotFoundException(String resourceType, Long id) {
        this(resourceType, String.valueOf(id));
    }
    
    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceType = "Resource";
        this.identifier = "unknown";
    }
    
    public String getResourceType() {
        return resourceType;
    }
    
    public String getIdentifier() {
        return identifier;
    }
}
