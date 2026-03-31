package com.example.workflowcommerce.service.workflow;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.workflowcommerce.dto.workflow.WorkflowDefinitionDTO;
import com.example.workflowcommerce.dto.workflow.WorkflowInstanceDTO;
import com.example.workflowcommerce.dto.workflow.WorkflowLogDTO;
import com.example.workflowcommerce.dto.workflow.WorkflowStateDTO;
import com.example.workflowcommerce.dto.workflow.WorkflowStatsDTO;
import com.example.workflowcommerce.dto.workflow.WorkflowTransitionDTO;
import com.example.workflowcommerce.exception.InvalidTransitionException;
import com.example.workflowcommerce.exception.ResourceNotFoundException;
import com.example.workflowcommerce.exception.UnauthorizedTransitionException;
import com.example.workflowcommerce.exception.WorkflowException;
import com.example.workflowcommerce.model.workflow.WorkflowDefinition;
import com.example.workflowcommerce.model.workflow.WorkflowInstance;
import com.example.workflowcommerce.model.workflow.WorkflowLog;
import com.example.workflowcommerce.model.workflow.WorkflowState;
import com.example.workflowcommerce.model.workflow.WorkflowTransition;
import com.example.workflowcommerce.repository.workflow.WorkflowDefinitionRepository;
import com.example.workflowcommerce.repository.workflow.WorkflowInstanceRepository;
import com.example.workflowcommerce.repository.workflow.WorkflowLogRepository;
import com.example.workflowcommerce.repository.workflow.WorkflowStateRepository;
import com.example.workflowcommerce.repository.workflow.WorkflowTransitionRepository;

/**
 * Core Workflow Engine Service
 * Manages workflow instances, state transitions, and audit logging.
 * This service is domain-independent and can be used for any entity type.
 * 
 * Production Features:
 * - Caching for frequently accessed workflow definitions
 * - Retry mechanism for transient failures
 * - Async notifications support
 */
@Service
@Transactional
public class WorkflowEngineService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowEngineService.class);

    @Autowired
    private WorkflowDefinitionRepository workflowDefinitionRepository;

    @Autowired
    private WorkflowStateRepository workflowStateRepository;

    @Autowired
    private WorkflowTransitionRepository workflowTransitionRepository;

    @Autowired
    private WorkflowInstanceRepository workflowInstanceRepository;

    @Autowired
    private WorkflowLogRepository workflowLogRepository;

    @Autowired
    private OrderWorkflowRuleValidator orderWorkflowRuleValidator;

    // ==================== WORKFLOW DEFINITION OPERATIONS ====================

    /**
     * Get all active workflow definitions (cached for performance)
     */
    @Cacheable(value = "workflowDefinitions", key = "'all'")
    public List<WorkflowDefinitionDTO> getAllWorkflowDefinitions() {
        logger.info("Fetching all workflow definitions from database (cache miss)");
        return workflowDefinitionRepository.findByActiveTrue()
                .stream()
                .map(this::toWorkflowDefinitionDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get workflow definition by name with full details (cached)
     */
    @Cacheable(value = "workflowDefinitions", key = "#name")
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public WorkflowDefinitionDTO getWorkflowDefinition(String name) {
        logger.info("Fetching workflow definition: {} (cache miss)", name);
        WorkflowDefinition workflow = workflowDefinitionRepository.findByNameWithStatesAndTransitions(name)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowDefinition", name));
        return toWorkflowDefinitionDTOWithDetails(workflow);
    }

    /**
     * Get workflow definition by ID with full details
     */
    public WorkflowDefinitionDTO getWorkflowDefinitionById(Long id) {
        WorkflowDefinition workflow = workflowDefinitionRepository.findByIdWithStatesAndTransitions(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowDefinition", id));
        return toWorkflowDefinitionDTOWithDetails(workflow);
    }

    // ==================== WORKFLOW INSTANCE OPERATIONS ====================

    /**
     * Create a new workflow instance for an entity
     */
    public WorkflowInstance createWorkflowInstance(String workflowName, String entityType, 
                                                    Long entityId, String createdBy) {
        // Find the workflow definition
        WorkflowDefinition workflow = workflowDefinitionRepository.findByNameAndActiveTrue(workflowName)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowDefinition", workflowName));

        // Check if active instance already exists
        Optional<WorkflowInstance> existingInstance = workflowInstanceRepository
                .findByEntityTypeAndEntityIdAndIsCompletedFalse(entityType, entityId);
        if (existingInstance.isPresent()) {
            throw new WorkflowException("INSTANCE_EXISTS", 
                    String.format("Active workflow instance already exists for %s:%d", entityType, entityId));
        }

        // Find initial state
        WorkflowState initialState = workflowStateRepository.findByWorkflowIdAndIsInitialTrue(workflow.getId())
                .orElseThrow(() -> new WorkflowException("NO_INITIAL_STATE", 
                        "No initial state defined for workflow: " + workflowName));

        // Create instance
        WorkflowInstance instance = new WorkflowInstance(workflow, entityType, entityId, initialState, createdBy);
        instance = workflowInstanceRepository.save(instance);

        // Log the creation
        WorkflowLog log = new WorkflowLog(instance, null, initialState, createdBy, "SYSTEM", 
                "Workflow Started", "Workflow instance created");
        workflowLogRepository.save(log);

        return instance;
    }

    /**
     * Get workflow instance by ID with full details
     */
    public WorkflowInstanceDTO getWorkflowInstance(Long instanceId, String userRole) {
        WorkflowInstance instance = workflowInstanceRepository.findByIdWithDetails(instanceId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowInstance", instanceId));
        return toWorkflowInstanceDTO(instance, userRole);
    }

    /**
     * Get active workflow instance for an entity
     */
    public WorkflowInstanceDTO getActiveWorkflowInstance(String entityType, Long entityId, String userRole) {
        WorkflowInstance instance = workflowInstanceRepository
                .findActiveByEntityWithDetails(entityType, entityId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowInstance", 
                        entityType + ":" + entityId));
        return toWorkflowInstanceDTO(instance, userRole);
    }

    /**
     * Get current state of a workflow instance
     */
    public WorkflowStateDTO getCurrentState(Long instanceId) {
        WorkflowInstance instance = workflowInstanceRepository.findByIdWithDetails(instanceId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowInstance", instanceId));
        return toWorkflowStateDTO(instance.getCurrentState());
    }

    /**
     * Get current state name of a workflow instance
     */
    public String getCurrentStateName(Long instanceId) {
        return getCurrentState(instanceId).getStateName();
    }

    /**
     * Get all active workflow instances
     */
    public List<WorkflowInstanceDTO> getAllActiveInstances(String userRole) {
        return workflowInstanceRepository.findAllActiveWithDetails()
                .stream()
                .map(instance -> toWorkflowInstanceDTO(instance, userRole))
                .collect(Collectors.toList());
    }

    /**
     * Get active instances by entity type
     */
    public List<WorkflowInstanceDTO> getInstancesByEntityType(String entityType, String userRole) {
        return workflowInstanceRepository.findByEntityTypeWithDetails(entityType)
                .stream()
                .map(instance -> toWorkflowInstanceDTO(instance, userRole))
                .collect(Collectors.toList());
    }

    // ==================== TRANSITION OPERATIONS ====================

    /**
     * Get allowed transitions for a workflow instance based on user role
     */
    public List<WorkflowTransitionDTO> getAllowedTransitions(Long instanceId, String userRole) {
        WorkflowInstance instance = workflowInstanceRepository.findByIdWithDetails(instanceId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowInstance", instanceId));

        if (instance.isCompleted()) {
            return Collections.emptyList();
        }

        List<WorkflowTransition> transitions = workflowTransitionRepository
                .findByFromStateIdWithStates(instance.getCurrentState().getId());

        return transitions.stream()
                .filter(t -> isRoleAllowed(t.getAllowedRoles(), userRole))
                .map(this::toWorkflowTransitionDTO)
                .collect(Collectors.toList());
    }

    /**
     * Execute a state transition
     */
    public WorkflowInstanceDTO transition(Long instanceId, String targetStateName, 
                                          String actor, String actorRole, String comment) {
        WorkflowInstance instance = workflowInstanceRepository.findByIdWithDetails(instanceId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowInstance", instanceId));

        if (instance.isCompleted()) {
            throw new WorkflowException("INSTANCE_COMPLETED", 
                    "Cannot transition a completed workflow instance");
        }

        WorkflowState currentState = instance.getCurrentState();
        
        // Find target state
        WorkflowState targetState = workflowStateRepository
                .findByWorkflowIdAndStateName(instance.getWorkflow().getId(), targetStateName)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowState", targetStateName));

        // Find and validate transition
        WorkflowTransition transition = workflowTransitionRepository
                .findByWorkflowIdAndFromStateIdAndToStateId(
                        instance.getWorkflow().getId(),
                        currentState.getId(),
                        targetState.getId())
                .orElseThrow(() -> new InvalidTransitionException(
                        currentState.getStateName(), targetStateName));

        // Check role permission
        if (!isRoleAllowed(transition.getAllowedRoles(), actorRole)) {
            throw new UnauthorizedTransitionException(actorRole, transition.getActionName());
        }

        // Check if comment is required
        if (transition.isRequiresComment() && (comment == null || comment.isBlank())) {
            throw new WorkflowException("COMMENT_REQUIRED", 
                    "Comment is required for this transition");
        }

        // Validate business rules before allowing transition
        orderWorkflowRuleValidator.validateTransition(
                instance.getEntityType(),
                instance.getEntityId(),
                currentState.getStateName(),
                targetStateName);

        // Perform transition
        instance.setCurrentState(targetState);
        
        // Check if terminal state
        if (targetState.isTerminal()) {
            instance.complete();
        }

        instance = workflowInstanceRepository.save(instance);

        // Log transition
        WorkflowLog log = new WorkflowLog(instance, currentState, targetState, 
                actor, actorRole, transition.getActionName(), comment);
        workflowLogRepository.save(log);

        return toWorkflowInstanceDTO(instance, actorRole);
    }

    /**
     * Execute a transition using entity type and entity ID
     */
    public WorkflowInstanceDTO transitionByEntity(String entityType, Long entityId, 
                                                   String targetStateName, String actor, 
                                                   String actorRole, String comment) {
        WorkflowInstance instance = workflowInstanceRepository
                .findActiveByEntityWithDetails(entityType, entityId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowInstance", 
                        entityType + ":" + entityId));
        
        return transition(instance.getId(), targetStateName, actor, actorRole, comment);
    }

    // ==================== AUDIT LOG OPERATIONS ====================

    /**
     * Get workflow logs for an instance
     */
    public List<WorkflowLogDTO> getWorkflowLogs(Long instanceId) {
        return workflowLogRepository.findByInstanceIdWithDetails(instanceId)
                .stream()
                .map(this::toWorkflowLogDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get workflow logs for an entity
     */
    public List<WorkflowLogDTO> getWorkflowLogsByEntity(String entityType, Long entityId) {
        return workflowLogRepository.findByEntityWithDetails(entityType, entityId)
                .stream()
                .map(this::toWorkflowLogDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get recent logs (last 24 hours)
     */
    public List<WorkflowLogDTO> getRecentLogs() {
        LocalDateTime since = LocalDateTime.now().minusDays(1);
        return workflowLogRepository.findRecentLogs(since)
                .stream()
                .map(this::toWorkflowLogDTO)
                .collect(Collectors.toList());
    }

    // ==================== STATISTICS OPERATIONS ====================

    /**
     * Get workflow statistics
     */
    public WorkflowStatsDTO getWorkflowStats(Long workflowId) {
        WorkflowStatsDTO stats = new WorkflowStatsDTO();

        // Count instances
        List<WorkflowInstance> instances = workflowInstanceRepository.findByWorkflowId(workflowId);
        stats.setTotalInstances(instances.size());
        stats.setActiveInstances(instances.stream().filter(i -> !i.isCompleted()).count());
        stats.setCompletedInstances(instances.stream().filter(WorkflowInstance::isCompleted).count());

        // Instances by state
        List<Object[]> stateCounts = workflowInstanceRepository.countByStateForWorkflow(workflowId);
        Map<String, Long> instancesByState = new HashMap<>();
        for (Object[] row : stateCounts) {
            instancesByState.put((String) row[0], (Long) row[1]);
        }
        stats.setInstancesByState(instancesByState);

        // Transitions today and this week
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime startOfWeek = LocalDateTime.now().minusDays(7);

        List<Object[]> todayTransitions = workflowLogRepository.countTransitionsByState(workflowId, startOfDay);
        stats.setTransitionsToday(todayTransitions.stream().mapToLong(r -> (Long) r[1]).sum());

        List<Object[]> weekTransitions = workflowLogRepository.countTransitionsByState(workflowId, startOfWeek);
        stats.setTransitionsThisWeek(weekTransitions.stream().mapToLong(r -> (Long) r[1]).sum());

        return stats;
    }

    // ==================== HELPER METHODS ====================

    private boolean isRoleAllowed(String allowedRoles, String userRole) {
        if (allowedRoles == null || allowedRoles.isEmpty()) {
            return false;
        }
        String[] roles = allowedRoles.split(",");
        for (String role : roles) {
            String trimmedRole = role.trim();
            if (trimmedRole.equalsIgnoreCase(userRole) || 
                trimmedRole.equalsIgnoreCase("SYSTEM") ||
                trimmedRole.equalsIgnoreCase("ANY")) {
                return true;
            }
        }
        return false;
    }

    // ==================== DTO MAPPERS ====================

    private WorkflowStateDTO toWorkflowStateDTO(WorkflowState state) {
        return new WorkflowStateDTO(
                state.getId(),
                state.getStateName(),
                state.getDescription(),
                state.isInitial(),
                state.isTerminal(),
                state.getDisplayOrder(),
                state.getColorCode()
        );
    }

    private WorkflowTransitionDTO toWorkflowTransitionDTO(WorkflowTransition transition) {
        return new WorkflowTransitionDTO(
                transition.getId(),
                transition.getFromState().getStateName(),
                transition.getToState().getStateName(),
                transition.getActionName(),
                transition.getDescription(),
                transition.getAllowedRoles(),
                transition.isRequiresComment(),
                transition.getToState().getColorCode()
        );
    }

    private WorkflowInstanceDTO toWorkflowInstanceDTO(WorkflowInstance instance, String userRole) {
        WorkflowInstanceDTO dto = new WorkflowInstanceDTO();
        dto.setId(instance.getId());
        dto.setWorkflowId(instance.getWorkflow().getId());
        dto.setWorkflowName(instance.getWorkflow().getName());
        dto.setEntityType(instance.getEntityType());
        dto.setEntityId(instance.getEntityId());
        dto.setCurrentState(toWorkflowStateDTO(instance.getCurrentState()));
        dto.setCompleted(instance.isCompleted());
        dto.setCreatedBy(instance.getCreatedBy());
        dto.setCreatedAt(instance.getCreatedAt());
        dto.setUpdatedAt(instance.getUpdatedAt());
        dto.setCompletedAt(instance.getCompletedAt());

        // Get allowed transitions
        if (!instance.isCompleted() && userRole != null) {
            dto.setAllowedTransitions(getAllowedTransitions(instance.getId(), userRole));
        } else {
            dto.setAllowedTransitions(Collections.emptyList());
        }

        // Get recent logs (last 5)
        List<WorkflowLogDTO> logs = workflowLogRepository.findByInstanceIdWithDetails(instance.getId())
                .stream()
                .limit(5)
                .map(this::toWorkflowLogDTO)
                .collect(Collectors.toList());
        dto.setRecentLogs(logs);

        return dto;
    }

    private WorkflowLogDTO toWorkflowLogDTO(WorkflowLog log) {
        WorkflowLogDTO dto = new WorkflowLogDTO();
        dto.setId(log.getId());
        dto.setWorkflowInstanceId(log.getWorkflowInstance().getId());
        dto.setEntityType(log.getWorkflowInstance().getEntityType());
        dto.setEntityId(log.getWorkflowInstance().getEntityId());

        if (log.getFromState() != null) {
            dto.setFromStateName(log.getFromState().getStateName());
            dto.setFromStateColorCode(log.getFromState().getColorCode());
        }

        dto.setToStateName(log.getToState().getStateName());
        dto.setToStateColorCode(log.getToState().getColorCode());
        dto.setPerformedBy(log.getPerformedBy());
        dto.setPerformedRole(log.getPerformedRole());
        dto.setActionName(log.getActionName());
        dto.setComment(log.getComment());
        dto.setTimestamp(log.getTimestamp());

        return dto;
    }

    private WorkflowDefinitionDTO toWorkflowDefinitionDTO(WorkflowDefinition workflow) {
        WorkflowDefinitionDTO dto = new WorkflowDefinitionDTO();
        dto.setId(workflow.getId());
        dto.setName(workflow.getName());
        dto.setDescription(workflow.getDescription());
        dto.setEntityType(workflow.getEntityType());
        dto.setActive(workflow.isActive());
        dto.setCreatedAt(workflow.getCreatedAt());
        return dto;
    }

    private WorkflowDefinitionDTO toWorkflowDefinitionDTOWithDetails(WorkflowDefinition workflow) {
        WorkflowDefinitionDTO dto = toWorkflowDefinitionDTO(workflow);

        // Add states
        List<WorkflowStateDTO> states = workflow.getStates().stream()
                .sorted(Comparator.comparing(WorkflowState::getDisplayOrder))
                .map(this::toWorkflowStateDTO)
                .collect(Collectors.toList());
        dto.setStates(states);

        // Add transitions
        List<WorkflowTransitionDTO> transitions = workflow.getTransitions().stream()
                .map(this::toWorkflowTransitionDTO)
                .collect(Collectors.toList());
        dto.setTransitions(transitions);

        // Add stats
        dto.setStats(getWorkflowStats(workflow.getId()));

        return dto;
    }
}
