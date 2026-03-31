package com.example.workflowcommerce.dto.workflow;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for workflow definition with states and transitions
 */
public class WorkflowDefinitionDTO {
    
    private Long id;
    private String name;
    private String description;
    private String entityType;
    private boolean active;
    private LocalDateTime createdAt;
    private List<WorkflowStateDTO> states;
    private List<WorkflowTransitionDTO> transitions;
    private WorkflowStatsDTO stats;
    
    public WorkflowDefinitionDTO() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public List<WorkflowStateDTO> getStates() { return states; }
    public void setStates(List<WorkflowStateDTO> states) { this.states = states; }
    
    public List<WorkflowTransitionDTO> getTransitions() { return transitions; }
    public void setTransitions(List<WorkflowTransitionDTO> transitions) { this.transitions = transitions; }
    
    public WorkflowStatsDTO getStats() { return stats; }
    public void setStats(WorkflowStatsDTO stats) { this.stats = stats; }
}
