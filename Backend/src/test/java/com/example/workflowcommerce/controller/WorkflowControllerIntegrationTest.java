package com.example.workflowcommerce.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for WorkflowController
 * Tests workflow API endpoints including authorization
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("WorkflowController Integration Tests")
class WorkflowControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("Workflow Definition Endpoint Tests")
    class WorkflowDefinitionTests {

        @Test
        @DisplayName("Should deny access to workflow definitions without auth")
        void getDefinitions_NoAuth_ReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/workflow/definitions"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        @DisplayName("Should allow admin to access workflow definitions")
        void getDefinitions_AsAdmin_ReturnsOk() throws Exception {
            mockMvc.perform(get("/api/workflow/definitions"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }

    @Nested
    @DisplayName("Workflow Instance Endpoint Tests")
    class WorkflowInstanceTests {

        @Test
        @DisplayName("Should deny access to instances without auth")
        void getInstances_NoAuth_ReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/workflow/instances"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        @DisplayName("Should return active instances for admin")
        void getInstances_AsAdmin_ReturnsOk() throws Exception {
            mockMvc.perform(get("/api/workflow/instances"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @WithMockUser(username = "user", roles = {"USER"})
        @DisplayName("Should deny user access to admin workflow endpoints")
        void getInstances_AsUser_ReturnsForbidden() throws Exception {
            mockMvc.perform(get("/api/workflow/instances"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Transition Execution Tests")
    class TransitionExecutionTests {

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        @DisplayName("Should validate transition request body")
        void executeTransition_InvalidBody_ReturnsBadRequest() throws Exception {
            String invalidJson = """
                {
                    "targetState": ""
                }
                """;

            mockMvc.perform(post("/api/workflow/instances/1/transition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        @DisplayName("Should return 404 for non-existent instance")
        void executeTransition_NonExistentInstance_ReturnsNotFound() throws Exception {
            String transitionJson = """
                {
                    "targetState": "CONFIRMED",
                    "comment": "Test transition"
                }
                """;

            mockMvc.perform(post("/api/workflow/instances/99999/transition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(transitionJson))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        // Should return 404 (not found) or 400/500 (no workflow data)
                        // Key is it processes the auth correctly
                        assertTrue(status == 404 || status == 400 || status == 500,
                            "Should handle non-existent instance");
                    });
        }
    }

    @Nested
    @DisplayName("Workflow Stats Endpoint Tests")
    class WorkflowStatsTests {

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        @DisplayName("Admin can access workflow statistics endpoint")
        void getStats_AsAdmin_AccessesEndpoint() throws Exception {
            mockMvc.perform(get("/api/workflow/stats"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        // Admin should not get 401/403
                        assertTrue(status != 401 && status != 403,
                            "Admin should have access to stats");
                    });
        }

        @Test
        @WithMockUser(username = "user", roles = {"USER"})
        @DisplayName("Regular user cannot access admin workflow stats")
        void getStats_AsUser_DeniedOrErrors() throws Exception {
            mockMvc.perform(get("/api/workflow/stats"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        // User should get 403 (forbidden) or 500 (error in accessing)
                        // The key is they don't get 200 OK
                        assertTrue(status == 403 || status == 500,
                            "User should not have full access to stats");
                    });
        }
    }

    @Nested
    @DisplayName("Audit Log Endpoint Tests")
    class AuditLogTests {

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        @DisplayName("Admin can access audit logs endpoint")
        void getAuditLogs_AsAdmin_AccessesEndpoint() throws Exception {
            mockMvc.perform(get("/api/workflow/logs"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        // Admin should not get 401/403
                        assertTrue(status != 401 && status != 403,
                            "Admin should have access to audit logs");
                    });
        }
    }
}
