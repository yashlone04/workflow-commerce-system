package com.example.workflowcommerce.repository.workflow;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.workflowcommerce.model.workflow.WorkflowInstance;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, Long> {

    List<WorkflowInstance> findByEntityTypeAndEntityId(String entityType, Long entityId);
    
    Optional<WorkflowInstance> findByEntityTypeAndEntityIdAndIsCompletedFalse(String entityType, Long entityId);
    
    List<WorkflowInstance> findByWorkflowId(Long workflowId);
    
    List<WorkflowInstance> findByCurrentStateId(Long currentStateId);
    
    List<WorkflowInstance> findByIsCompletedFalse();
    
    List<WorkflowInstance> findByIsCompletedTrue();
    
    @Query("SELECT wi FROM WorkflowInstance wi " +
           "JOIN FETCH wi.workflow " +
           "JOIN FETCH wi.currentState " +
           "WHERE wi.id = :id")
    Optional<WorkflowInstance> findByIdWithDetails(@Param("id") Long id);
    
    @Query("SELECT wi FROM WorkflowInstance wi " +
           "JOIN FETCH wi.workflow " +
           "JOIN FETCH wi.currentState " +
           "WHERE wi.entityType = :entityType AND wi.entityId = :entityId AND wi.isCompleted = false")
    Optional<WorkflowInstance> findActiveByEntityWithDetails(@Param("entityType") String entityType, 
                                                              @Param("entityId") Long entityId);
    
    @Query("SELECT wi FROM WorkflowInstance wi " +
           "JOIN FETCH wi.workflow " +
           "JOIN FETCH wi.currentState " +
           "WHERE wi.isCompleted = false " +
           "ORDER BY wi.updatedAt DESC")
    List<WorkflowInstance> findAllActiveWithDetails();
    
    @Query("SELECT wi FROM WorkflowInstance wi " +
           "JOIN FETCH wi.workflow " +
           "JOIN FETCH wi.currentState " +
           "WHERE wi.workflow.id = :workflowId AND wi.isCompleted = false " +
           "ORDER BY wi.updatedAt DESC")
    List<WorkflowInstance> findActiveByWorkflowIdWithDetails(@Param("workflowId") Long workflowId);
    
    @Query("SELECT wi.currentState.stateName, COUNT(wi) FROM WorkflowInstance wi " +
           "WHERE wi.workflow.id = :workflowId AND wi.isCompleted = false " +
           "GROUP BY wi.currentState.stateName")
    List<Object[]> countByStateForWorkflow(@Param("workflowId") Long workflowId);
    
    @Query("SELECT COUNT(wi) FROM WorkflowInstance wi WHERE wi.entityType = :entityType AND wi.isCompleted = false")
    long countActiveByEntityType(@Param("entityType") String entityType);
    
    @Query("SELECT wi FROM WorkflowInstance wi " +
           "JOIN FETCH wi.workflow " +
           "JOIN FETCH wi.currentState " +
           "WHERE wi.entityType = :entityType " +
           "ORDER BY wi.updatedAt DESC")
    List<WorkflowInstance> findByEntityTypeWithDetails(@Param("entityType") String entityType);
    
    @Query("SELECT wi FROM WorkflowInstance wi " +
           "WHERE wi.createdAt >= :since " +
           "ORDER BY wi.createdAt DESC")
    List<WorkflowInstance> findRecentInstances(@Param("since") LocalDateTime since);
}
