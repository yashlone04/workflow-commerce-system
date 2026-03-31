package com.example.workflowcommerce.repository.workflow;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.workflowcommerce.model.workflow.WorkflowState;

@Repository
public interface WorkflowStateRepository extends JpaRepository<WorkflowState, Long> {

    List<WorkflowState> findByWorkflowIdOrderByDisplayOrderAsc(Long workflowId);
    
    Optional<WorkflowState> findByWorkflowIdAndStateName(Long workflowId, String stateName);
    
    Optional<WorkflowState> findByWorkflowIdAndIsInitialTrue(Long workflowId);
    
    List<WorkflowState> findByWorkflowIdAndIsTerminalTrue(Long workflowId);
    
    @Query("SELECT ws FROM WorkflowState ws WHERE ws.workflow.name = :workflowName AND ws.stateName = :stateName")
    Optional<WorkflowState> findByWorkflowNameAndStateName(@Param("workflowName") String workflowName, 
                                                           @Param("stateName") String stateName);
    
    @Query("SELECT ws FROM WorkflowState ws WHERE ws.workflow.name = :workflowName AND ws.isInitial = true")
    Optional<WorkflowState> findInitialStateByWorkflowName(@Param("workflowName") String workflowName);
    
    @Query("SELECT COUNT(ws) FROM WorkflowState ws WHERE ws.workflow.id = :workflowId")
    long countByWorkflowId(@Param("workflowId") Long workflowId);
}
