package com.example.workflowcommerce.repository.workflow;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.workflowcommerce.model.workflow.WorkflowDefinition;

@Repository
public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, Long> {

    Optional<WorkflowDefinition> findByName(String name);
    
    Optional<WorkflowDefinition> findByNameAndActiveTrue(String name);
    
    List<WorkflowDefinition> findByActiveTrue();
    
    List<WorkflowDefinition> findByEntityType(String entityType);
    
    List<WorkflowDefinition> findByEntityTypeAndActiveTrue(String entityType);
    
    boolean existsByName(String name);
    
    @Query("SELECT wd FROM WorkflowDefinition wd " +
           "LEFT JOIN FETCH wd.states " +
           "LEFT JOIN FETCH wd.transitions " +
           "WHERE wd.id = :id")
    Optional<WorkflowDefinition> findByIdWithStatesAndTransitions(@Param("id") Long id);
    
    @Query("SELECT wd FROM WorkflowDefinition wd " +
           "LEFT JOIN FETCH wd.states " +
           "LEFT JOIN FETCH wd.transitions " +
           "WHERE wd.name = :name AND wd.active = true")
    Optional<WorkflowDefinition> findByNameWithStatesAndTransitions(@Param("name") String name);
}
