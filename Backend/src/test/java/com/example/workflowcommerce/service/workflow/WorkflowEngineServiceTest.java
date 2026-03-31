package com.example.workflowcommerce.service.workflow;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.workflowcommerce.dto.workflow.WorkflowDefinitionDTO;
import com.example.workflowcommerce.dto.workflow.WorkflowInstanceDTO;
import com.example.workflowcommerce.exception.ResourceNotFoundException;
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
 * Unit tests for WorkflowEngineService
 * Tests the core workflow engine logic including state transitions,
 * validation, and authorization.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowEngineService Unit Tests")
class WorkflowEngineServiceTest {

    @Mock
    private WorkflowDefinitionRepository workflowDefinitionRepository;

    @Mock
    private WorkflowStateRepository workflowStateRepository;

    @Mock
    private WorkflowTransitionRepository workflowTransitionRepository;

    @Mock
    private WorkflowInstanceRepository workflowInstanceRepository;

    @Mock
    private WorkflowLogRepository workflowLogRepository;

    @Mock
    private OrderWorkflowRuleValidator orderWorkflowRuleValidator;

    @InjectMocks
    private WorkflowEngineService workflowEngineService;

    // Test fixtures
    private WorkflowDefinition orderWorkflow;
    private WorkflowState pendingState;
    private WorkflowState confirmedState;
    private WorkflowState shippedState;
    private WorkflowTransition pendingToConfirmedTransition;
    private WorkflowInstance testInstance;

    @BeforeEach
    void setUp() {
        // Setup workflow definition
        orderWorkflow = new WorkflowDefinition();
        orderWorkflow.setId(1L);
        orderWorkflow.setName("ORDER_WORKFLOW");
        orderWorkflow.setActive(true);

        // Setup states
        pendingState = new WorkflowState();
        pendingState.setId(1L);
        pendingState.setStateName("PENDING");
        pendingState.setInitial(true);
        pendingState.setTerminal(false);
        pendingState.setWorkflow(orderWorkflow);

        confirmedState = new WorkflowState();
        confirmedState.setId(2L);
        confirmedState.setStateName("CONFIRMED");
        confirmedState.setInitial(false);
        confirmedState.setTerminal(false);
        confirmedState.setWorkflow(orderWorkflow);

        shippedState = new WorkflowState();
        shippedState.setId(3L);
        shippedState.setStateName("SHIPPED");
        shippedState.setInitial(false);
        shippedState.setTerminal(false);
        shippedState.setWorkflow(orderWorkflow);

        // Setup transitions
        pendingToConfirmedTransition = new WorkflowTransition();
        pendingToConfirmedTransition.setId(1L);
        pendingToConfirmedTransition.setFromState(pendingState);
        pendingToConfirmedTransition.setToState(confirmedState);
        pendingToConfirmedTransition.setAllowedRoles("ROLE_ADMIN");
        pendingToConfirmedTransition.setWorkflow(orderWorkflow);

        // Setup test instance
        testInstance = new WorkflowInstance();
        testInstance.setId(1L);
        testInstance.setWorkflow(orderWorkflow);
        testInstance.setEntityType("Order");
        testInstance.setEntityId(100L);
        testInstance.setCurrentState(pendingState);
        testInstance.setCreatedBy("testuser");
        testInstance.setCompleted(false);
    }

    @Nested
    @DisplayName("Workflow Definition Tests")
    class WorkflowDefinitionTests {

        @Test
        @DisplayName("Should return all active workflow definitions")
        void getAllWorkflowDefinitions_ReturnsActiveWorkflows() {
            // Given
            when(workflowDefinitionRepository.findByActiveTrue())
                    .thenReturn(Arrays.asList(orderWorkflow));

            // When
            List<WorkflowDefinitionDTO> result = workflowEngineService.getAllWorkflowDefinitions();

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("ORDER_WORKFLOW", result.get(0).getName());
            verify(workflowDefinitionRepository, times(1)).findByActiveTrue();
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException for non-existent workflow")
        void getWorkflowDefinition_NonExistent_ThrowsException() {
            // Given
            when(workflowDefinitionRepository.findByNameWithStatesAndTransitions("INVALID"))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThrows(ResourceNotFoundException.class, () -> {
                workflowEngineService.getWorkflowDefinition("INVALID");
            });
        }
    }

    @Nested
    @DisplayName("Workflow Instance Creation Tests")
    class WorkflowInstanceCreationTests {

        @Test
        @DisplayName("Should create workflow instance successfully")
        void createWorkflowInstance_Success() {
            // Given
            when(workflowDefinitionRepository.findByNameAndActiveTrue("ORDER_WORKFLOW"))
                    .thenReturn(Optional.of(orderWorkflow));
            when(workflowInstanceRepository.findByEntityTypeAndEntityIdAndIsCompletedFalse("Order", 100L))
                    .thenReturn(Optional.empty());
            when(workflowStateRepository.findByWorkflowIdAndIsInitialTrue(1L))
                    .thenReturn(Optional.of(pendingState));
            when(workflowInstanceRepository.save(any(WorkflowInstance.class)))
                    .thenReturn(testInstance);
            when(workflowLogRepository.save(any(WorkflowLog.class)))
                    .thenReturn(new WorkflowLog());

            // When
            WorkflowInstance result = workflowEngineService.createWorkflowInstance(
                    "ORDER_WORKFLOW", "Order", 100L, "testuser");

            // Then
            assertNotNull(result);
            assertEquals("Order", result.getEntityType());
            assertEquals(100L, result.getEntityId());
            assertEquals("PENDING", result.getCurrentState().getStateName());
            verify(workflowInstanceRepository, times(1)).save(any(WorkflowInstance.class));
            verify(workflowLogRepository, times(1)).save(any(WorkflowLog.class));
        }

        @Test
        @DisplayName("Should throw exception when instance already exists")
        void createWorkflowInstance_AlreadyExists_ThrowsException() {
            // Given
            when(workflowDefinitionRepository.findByNameAndActiveTrue("ORDER_WORKFLOW"))
                    .thenReturn(Optional.of(orderWorkflow));
            when(workflowInstanceRepository.findByEntityTypeAndEntityIdAndIsCompletedFalse("Order", 100L))
                    .thenReturn(Optional.of(testInstance));

            // When & Then
            WorkflowException exception = assertThrows(WorkflowException.class, () -> {
                workflowEngineService.createWorkflowInstance("ORDER_WORKFLOW", "Order", 100L, "testuser");
            });
            assertEquals("INSTANCE_EXISTS", exception.getErrorCode());
        }

        @Test
        @DisplayName("Should throw exception when workflow has no initial state")
        void createWorkflowInstance_NoInitialState_ThrowsException() {
            // Given
            when(workflowDefinitionRepository.findByNameAndActiveTrue("ORDER_WORKFLOW"))
                    .thenReturn(Optional.of(orderWorkflow));
            when(workflowInstanceRepository.findByEntityTypeAndEntityIdAndIsCompletedFalse("Order", 100L))
                    .thenReturn(Optional.empty());
            when(workflowStateRepository.findByWorkflowIdAndIsInitialTrue(1L))
                    .thenReturn(Optional.empty());

            // When & Then
            WorkflowException exception = assertThrows(WorkflowException.class, () -> {
                workflowEngineService.createWorkflowInstance("ORDER_WORKFLOW", "Order", 100L, "testuser");
            });
            assertEquals("NO_INITIAL_STATE", exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("State Transition Tests")
    class StateTransitionTests {

        @Test
        @DisplayName("Should get available transitions for user role")
        void getAllowedTransitions_ReturnsRoleBasedTransitions() {
            // Given
            when(workflowInstanceRepository.findByIdWithDetails(1L))
                    .thenReturn(Optional.of(testInstance));
            when(workflowTransitionRepository.findByFromStateIdWithStates(1L))
                    .thenReturn(Arrays.asList(pendingToConfirmedTransition));

            // When
            var result = workflowEngineService.getAllowedTransitions(1L, "ROLE_ADMIN");

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should filter transitions by role")
        void getAllowedTransitions_FiltersNonMatchingRoles() {
            // Given
            when(workflowInstanceRepository.findByIdWithDetails(1L))
                    .thenReturn(Optional.of(testInstance));
            when(workflowTransitionRepository.findByFromStateIdWithStates(1L))
                    .thenReturn(Arrays.asList(pendingToConfirmedTransition));

            // When - User without required role
            var result = workflowEngineService.getAllowedTransitions(1L, "ROLE_USER");

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should throw exception for invalid transition target")
        void transition_InvalidTarget_ThrowsException() {
            // Given
            when(workflowInstanceRepository.findByIdWithDetails(1L))
                    .thenReturn(Optional.of(testInstance));
            when(workflowStateRepository.findByWorkflowIdAndStateName(1L, "INVALID_STATE"))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThrows(ResourceNotFoundException.class, () -> {
                workflowEngineService.transition(1L, "INVALID_STATE", "admin", "ROLE_ADMIN", "Test comment");
            });
        }
    }

    @Nested
    @DisplayName("Workflow Instance Query Tests")
    class WorkflowInstanceQueryTests {

        @Test
        @DisplayName("Should get workflow instance by ID")
        void getWorkflowInstance_ReturnsInstance() {
            // Given
            when(workflowInstanceRepository.findByIdWithDetails(1L))
                    .thenReturn(Optional.of(testInstance));
            when(workflowTransitionRepository.findByFromStateIdWithStates(anyLong()))
                    .thenReturn(Arrays.asList());

            // When
            WorkflowInstanceDTO result = workflowEngineService.getWorkflowInstance(1L, "ROLE_ADMIN");

            // Then
            assertNotNull(result);
            assertEquals("Order", result.getEntityType());
            assertEquals(100L, result.getEntityId());
            assertNotNull(result.getCurrentState());
            assertEquals("PENDING", result.getCurrentState().getStateName());
        }

        @Test
        @DisplayName("Should throw exception for non-existent instance")
        void getWorkflowInstance_NonExistent_ThrowsException() {
            // Given
            when(workflowInstanceRepository.findByIdWithDetails(999L))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThrows(ResourceNotFoundException.class, () -> {
                workflowEngineService.getWorkflowInstance(999L, "ROLE_ADMIN");
            });
        }
    }

    @Nested
    @DisplayName("Edge Cases and Validation Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle completed workflow instance correctly")
        void completedWorkflow_NoTransitionsAvailable() {
            // Given
            testInstance.setCompleted(true);
            when(workflowInstanceRepository.findByIdWithDetails(1L))
                    .thenReturn(Optional.of(testInstance));

            // When
            var result = workflowEngineService.getAllowedTransitions(1L, "ROLE_ADMIN");

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should handle null comment gracefully")
        void transition_NullComment_AcceptsGracefully() {
            // Given
            when(workflowInstanceRepository.findByIdWithDetails(1L))
                    .thenReturn(Optional.of(testInstance));
            when(workflowStateRepository.findByWorkflowIdAndStateName(1L, "CONFIRMED"))
                    .thenReturn(Optional.of(confirmedState));
            when(workflowTransitionRepository.findByWorkflowIdAndFromStateIdAndToStateId(1L, 1L, 2L))
                    .thenReturn(Optional.of(pendingToConfirmedTransition));
            when(workflowInstanceRepository.save(any(WorkflowInstance.class)))
                    .thenReturn(testInstance);
            when(workflowLogRepository.save(any(WorkflowLog.class)))
                    .thenReturn(new WorkflowLog());
            doNothing().when(orderWorkflowRuleValidator).validateTransition(anyString(), anyLong(), anyString(), any());

            // When & Then - Should not throw
            assertDoesNotThrow(() -> {
                workflowEngineService.transition(1L, "CONFIRMED", "admin", "ROLE_ADMIN", null);
            });
        }
    }
}
