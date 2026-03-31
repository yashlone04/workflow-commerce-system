import axios from "axios";
import authHeader from "./auth-header";

// Use base API URL without /auth suffix for workflow endpoints
const getBaseUrl = () => {
    const envUrl = import.meta.env.VITE_API_URL || "http://localhost:8080/api/auth/";
    // Remove trailing /auth/ or /auth to get base API URL
    return envUrl.replace(/\/auth\/?$/, '');
};

const API_URL = getBaseUrl();

class WorkflowService {
    // ============== Workflow Definitions ==============

    getAllDefinitions() {
        return axios.get(`${API_URL}/workflow/definitions`, { headers: authHeader() });
    }

    getDefinitionByName(name) {
        return axios.get(`${API_URL}/workflow/definitions/${name}`, { headers: authHeader() });
    }

    getWorkflowStats(workflowId) {
        return axios.get(`${API_URL}/workflow/definitions/${workflowId}/stats`, { headers: authHeader() });
    }

    // ============== Workflow Instances ==============

    getAllInstances() {
        return axios.get(`${API_URL}/workflow/instances`, { headers: authHeader() });
    }

    getInstanceById(instanceId) {
        return axios.get(`${API_URL}/workflow/instances/${instanceId}`, { headers: authHeader() });
    }

    getInstanceByEntity(entityType, entityId) {
        return axios.get(`${API_URL}/workflow/instances/entity/${entityType}/${entityId}`, { headers: authHeader() });
    }

    createInstance(workflowName, entityType, entityId) {
        return axios.post(`${API_URL}/workflow/instances`, null, {
            headers: authHeader(),
            params: { workflowName, entityType, entityId }
        });
    }

    // ============== Transitions ==============

    getAllowedTransitions(instanceId) {
        return axios.get(`${API_URL}/workflow/instances/${instanceId}/transitions`, { headers: authHeader() });
    }

    performTransition(instanceId, targetState, comment = "") {
        return axios.post(`${API_URL}/workflow/instances/${instanceId}/transition`, {
            targetState,
            comment
        }, { headers: authHeader() });
    }

    // ============== Workflow Logs ==============

    getInstanceLogs(instanceId) {
        return axios.get(`${API_URL}/workflow/instances/${instanceId}/logs`, { headers: authHeader() });
    }

    getEntityLogs(entityType, entityId) {
        return axios.get(`${API_URL}/workflow/logs/entity/${entityType}/${entityId}`, { headers: authHeader() });
    }

    getRecentLogs(limit = 50) {
        return axios.get(`${API_URL}/workflow/logs/recent`, {
            headers: authHeader(),
            params: { limit }
        });
    }

    // ============== Order Workflow Shortcuts ==============

    getOrderWorkflowInstance(orderId) {
        return this.getInstanceByEntity("ORDER", orderId);
    }

    transitionOrder(orderId, targetState, comment = "") {
        return this.getOrderWorkflowInstance(orderId)
            .then(response => {
                const instanceId = response.data.data?.id || response.data.id;
                return this.performTransition(instanceId, targetState, comment);
            });
    }

    // ============== Migration / Admin Tools ==============

    migrateAllOrders() {
        return axios.post(`${API_URL}/workflow/migrate/orders`, null, { headers: authHeader() });
    }
}

const workflowService = new WorkflowService();
export default workflowService;
