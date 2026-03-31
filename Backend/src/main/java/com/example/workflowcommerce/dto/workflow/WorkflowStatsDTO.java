package com.example.workflowcommerce.dto.workflow;

import java.util.Map;

/**
 * DTO for workflow statistics
 */
public class WorkflowStatsDTO {
    
    private long totalInstances;
    private long activeInstances;
    private long completedInstances;
    private Map<String, Long> instancesByState;
    private long transitionsToday;
    private long transitionsThisWeek;
    
    public WorkflowStatsDTO() {}
    
    // Getters and Setters
    public long getTotalInstances() { return totalInstances; }
    public void setTotalInstances(long totalInstances) { this.totalInstances = totalInstances; }
    
    public long getActiveInstances() { return activeInstances; }
    public void setActiveInstances(long activeInstances) { this.activeInstances = activeInstances; }
    
    public long getCompletedInstances() { return completedInstances; }
    public void setCompletedInstances(long completedInstances) { this.completedInstances = completedInstances; }
    
    public Map<String, Long> getInstancesByState() { return instancesByState; }
    public void setInstancesByState(Map<String, Long> instancesByState) { this.instancesByState = instancesByState; }
    
    public long getTransitionsToday() { return transitionsToday; }
    public void setTransitionsToday(long transitionsToday) { this.transitionsToday = transitionsToday; }
    
    public long getTransitionsThisWeek() { return transitionsThisWeek; }
    public void setTransitionsThisWeek(long transitionsThisWeek) { this.transitionsThisWeek = transitionsThisWeek; }
}
