package com.example.workflowcommerce.service.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Executes workflow transitions in a NEW transaction.
 * This ensures that workflow transition failures don't roll back
 * the parent business transaction (e.g., shipping creation).
 */
@Service
public class WorkflowTransitionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowTransitionExecutor.class);

    @Autowired
    private WorkflowEngineService workflowEngineService;

    /**
     * Execute a workflow transition in a separate transaction.
     * Returns true if successful, false otherwise.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean executeTransition(String entityType, Long entityId, 
                                     String targetState, String actor, 
                                     String actorRole, String comment) {
        try {
            workflowEngineService.transitionByEntity(entityType, entityId, targetState, 
                    actor, actorRole, comment);
            logger.info("Workflow transition to {} completed for {}:{}", 
                    targetState, entityType, entityId);
            return true;
        } catch (Exception e) {
            logger.warn("Workflow transition to {} failed for {}:{}: {}", 
                    targetState, entityType, entityId, e.getMessage());
            // Don't rethrow - we want this transaction to commit (or rollback independently)
            return false;
        }
    }

    /**
     * Create a workflow instance in a separate transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean createWorkflowInstance(String workflowName, String entityType, 
                                          Long entityId, String createdBy) {
        try {
            workflowEngineService.createWorkflowInstance(workflowName, entityType, 
                    entityId, createdBy);
            logger.info("Workflow instance created for {}:{}", entityType, entityId);
            return true;
        } catch (Exception e) {
            logger.warn("Failed to create workflow instance for {}:{}: {}", 
                    entityType, entityId, e.getMessage());
            return false;
        }
    }

    /**
     * Create workflow instance AND perform initial transition in the same transaction.
     * This ensures the workflow instance exists before transitioning.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean createWorkflowInstanceWithTransition(String workflowName, String entityType, 
                                                        Long entityId, String createdBy,
                                                        String initialTargetState, String comment) {
        try {
            // Create instance
            workflowEngineService.createWorkflowInstance(workflowName, entityType, 
                    entityId, createdBy);
            logger.info("Workflow instance created for {}:{}", entityType, entityId);
            
            // Immediately transition to initial target state
            workflowEngineService.transitionByEntity(entityType, entityId, initialTargetState, 
                    "SYSTEM", "ROLE_ADMIN", comment);
            logger.info("Workflow transitioned to {} for {}:{}", initialTargetState, entityType, entityId);
            
            return true;
        } catch (Exception e) {
            logger.warn("Failed to create/transition workflow for {}:{}: {}", 
                    entityType, entityId, e.getMessage());
            return false;
        }
    }

    /**
     * Execute multiple transitions in sequence within the same transaction.
     * Used for payment completed: PAID → PROCESSING
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean executeMultipleTransitions(String entityType, Long entityId, 
                                              String[] targetStates, String[] comments) {
        try {
            for (int i = 0; i < targetStates.length; i++) {
                String targetState = targetStates[i];
                String comment = (i < comments.length) ? comments[i] : "";
                
                workflowEngineService.transitionByEntity(entityType, entityId, targetState, 
                        "SYSTEM", "ROLE_ADMIN", comment);
                logger.info("Workflow transitioned to {} for {}:{}", targetState, entityType, entityId);
            }
            return true;
        } catch (Exception e) {
            logger.warn("Multi-transition failed for {}:{}: {}", 
                    entityType, entityId, e.getMessage());
            return false;
        }
    }
}
