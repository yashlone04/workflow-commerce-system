package com.example.workflowcommerce.repository.workflow;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.workflowcommerce.model.workflow.WorkflowLog;

@Repository
public interface WorkflowLogRepository extends JpaRepository<WorkflowLog, Long> {

    List<WorkflowLog> findByWorkflowInstanceIdOrderByTimestampDesc(Long workflowInstanceId);
    
    Page<WorkflowLog> findByWorkflowInstanceIdOrderByTimestampDesc(Long workflowInstanceId, Pageable pageable);
    
    List<WorkflowLog> findByPerformedByOrderByTimestampDesc(String performedBy);
    
    @Query("SELECT wl FROM WorkflowLog wl " +
           "JOIN FETCH wl.workflowInstance wi " +
           "JOIN FETCH wl.toState " +
           "LEFT JOIN FETCH wl.fromState " +
           "WHERE wi.id = :instanceId " +
           "ORDER BY wl.timestamp DESC")
    List<WorkflowLog> findByInstanceIdWithDetails(@Param("instanceId") Long instanceId);
    
    @Query("SELECT wl FROM WorkflowLog wl " +
           "JOIN FETCH wl.workflowInstance wi " +
           "JOIN FETCH wl.toState " +
           "LEFT JOIN FETCH wl.fromState " +
           "WHERE wl.timestamp >= :since " +
           "ORDER BY wl.timestamp DESC")
    List<WorkflowLog> findRecentLogs(@Param("since") LocalDateTime since);
    
    @Query("SELECT wl FROM WorkflowLog wl " +
           "JOIN FETCH wl.workflowInstance wi " +
           "JOIN FETCH wl.toState " +
           "LEFT JOIN FETCH wl.fromState " +
           "WHERE wi.entityType = :entityType AND wi.entityId = :entityId " +
           "ORDER BY wl.timestamp DESC")
    List<WorkflowLog> findByEntityWithDetails(@Param("entityType") String entityType, 
                                               @Param("entityId") Long entityId);
    
    @Query("SELECT wl.performedBy, COUNT(wl) FROM WorkflowLog wl " +
           "WHERE wl.timestamp >= :since " +
           "GROUP BY wl.performedBy " +
           "ORDER BY COUNT(wl) DESC")
    List<Object[]> countActionsByActor(@Param("since") LocalDateTime since);
    
    @Query("SELECT wl.toState.stateName, COUNT(wl) FROM WorkflowLog wl " +
           "JOIN wl.workflowInstance wi " +
           "WHERE wi.workflow.id = :workflowId AND wl.timestamp >= :since " +
           "GROUP BY wl.toState.stateName")
    List<Object[]> countTransitionsByState(@Param("workflowId") Long workflowId, 
                                           @Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(wl) FROM WorkflowLog wl WHERE wl.timestamp >= :since")
    long countSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT wl FROM WorkflowLog wl " +
           "JOIN FETCH wl.workflowInstance wi " +
           "JOIN FETCH wl.toState " +
           "LEFT JOIN FETCH wl.fromState " +
           "ORDER BY wl.timestamp DESC")
    Page<WorkflowLog> findAllWithDetails(Pageable pageable);
}
