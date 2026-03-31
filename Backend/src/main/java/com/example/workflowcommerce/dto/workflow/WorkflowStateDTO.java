package com.example.workflowcommerce.dto.workflow;

/**
 * DTO for workflow state information
 */
public class WorkflowStateDTO {
    
    private Long id;
    private String stateName;
    private String description;
    private boolean isInitial;
    private boolean isTerminal;
    private Integer displayOrder;
    private String colorCode;
    
    public WorkflowStateDTO() {}
    
    public WorkflowStateDTO(Long id, String stateName, String description, 
                            boolean isInitial, boolean isTerminal, Integer displayOrder, String colorCode) {
        this.id = id;
        this.stateName = stateName;
        this.description = description;
        this.isInitial = isInitial;
        this.isTerminal = isTerminal;
        this.displayOrder = displayOrder;
        this.colorCode = colorCode;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getStateName() { return stateName; }
    public void setStateName(String stateName) { this.stateName = stateName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public boolean isInitial() { return isInitial; }
    public void setInitial(boolean initial) { isInitial = initial; }
    
    public boolean isTerminal() { return isTerminal; }
    public void setTerminal(boolean terminal) { isTerminal = terminal; }
    
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    
    public String getColorCode() { return colorCode; }
    public void setColorCode(String colorCode) { this.colorCode = colorCode; }
}
