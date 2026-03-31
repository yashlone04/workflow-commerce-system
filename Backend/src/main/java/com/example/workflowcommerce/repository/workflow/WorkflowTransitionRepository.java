package com.example.workflowcommerce.repository.workflow;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.workflowcommerce.model.workflow.WorkflowTransition;

@Repository
public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, Long> {

    List<WorkflowTransition> findByWorkflowId(Long workflowId);
    
    List<WorkflowTransition> findByFromStateId(Long fromStateId);
    
    @Query("SELECT wt FROM WorkflowTransition wt " +
           "WHERE wt.workflow.id = :workflowId AND wt.fromState.id = :fromStateId")
    List<WorkflowTransition> findByWorkflowIdAndFromStateId(@Param("workflowId") Long workflowId, 
                                                             @Param("fromStateId") Long fromStateId);
    
    @Query("SELECT wt FROM WorkflowTransition wt " +
           "WHERE wt.workflow.id = :workflowId " +
           "AND wt.fromState.id = :fromStateId " +
           "AND wt.toState.id = :toStateId")
    Optional<WorkflowTransition> findByWorkflowIdAndFromStateIdAndToStateId(
            @Param("workflowId") Long workflowId,
            @Param("fromStateId") Long fromStateId,
            @Param("toStateId") Long toStateId);
    
    @Query("SELECT wt FROM WorkflowTransition wt " +
           "WHERE wt.workflow.id = :workflowId " +
           "AND wt.fromState.stateName = :fromStateName " +
           "AND wt.toState.stateName = :toStateName")
    Optional<WorkflowTransition> findByWorkflowIdAndStateNames(
            @Param("workflowId") Long workflowId,
            @Param("fromStateName") String fromStateName,
            @Param("toStateName") String toStateName);
    
    @Query("SELECT wt FROM WorkflowTransition wt " +
           "JOIN FETCH wt.fromState " +
           "JOIN FETCH wt.toState " +
           "WHERE wt.fromState.id = :fromStateId")
    List<WorkflowTransition> findByFromStateIdWithStates(@Param("fromStateId") Long fromStateId);
    
    @Query("SELECT COUNT(wt) FROM WorkflowTransition wt WHERE wt.workflow.id = :workflowId")
    long countByWorkflowId(@Param("workflowId") Long workflowId);
}
