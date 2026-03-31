import React, { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import workflowService from "../../services/workflow.service";

const WorkflowsPage = () => {
    const [definitions, setDefinitions] = useState([]);
    const [instances, setInstances] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [activeTab, setActiveTab] = useState("instances");
    const [statusFilter, setStatusFilter] = useState("active");
    const [searchTerm, setSearchTerm] = useState("");

    useEffect(() => {
        fetchData();
    }, []);

    const fetchData = async () => {
        try {
            setLoading(true);
            const [defsRes, instancesRes] = await Promise.all([
                workflowService.getAllDefinitions(),
                workflowService.getAllInstances()
            ]);
            setDefinitions(defsRes.data?.data || defsRes.data || []);
            setInstances(instancesRes.data?.data || instancesRes.data || []);
        } catch (err) {
            console.error("Failed to fetch workflows:", err);
            setError("Failed to load workflows");
        } finally {
            setLoading(false);
        }
    };

    const isTerminalState = (state) => {
        const terminalStates = ['DELIVERED', 'CANCELLED', 'REFUNDED', 'COMPLETED', 'FAILED', 'RETURNED'];
        return terminalStates.includes(state);
    };

    const getStateColor = (state) => {
        const colors = {
            'CREATED': '#6c757d',
            'PAYMENT_PENDING': '#fd7e14',
            'PAID': '#198754',
            'PROCESSING': '#0d6efd',
            'SHIPPED': '#6f42c1',
            'DELIVERED': '#198754',
            'CANCELLED': '#dc3545',
            'REFUNDED': '#ffc107',
            'INITIATED': '#6c757d',
            'COMPLETED': '#198754',
            'FAILED': '#dc3545',
            'IN_TRANSIT': '#0d6efd',
            'OUT_FOR_DELIVERY': '#6f42c1',
            'PENDING': '#fd7e14',
            'PICKED_UP': '#198754'
        };
        return colors[state] || '#6c757d';
    };

    const formatTimeAgo = (timestamp) => {
        const now = new Date();
        const then = new Date(timestamp);
        const diffMs = now - then;
        const diffMins = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMins / 60);
        const diffDays = Math.floor(diffHours / 24);

        if (diffMins < 1) return 'Just now';
        if (diffMins < 60) return `${diffMins}m ago`;
        if (diffHours < 24) return `${diffHours}h ago`;
        return `${diffDays}d ago`;
    };

    const filteredInstances = instances.filter(instance => {
        const matchesStatus = statusFilter === "all"
            || (statusFilter === "active" && !isTerminalState(instance.currentStateName))
            || (statusFilter === "completed" && isTerminalState(instance.currentStateName));

        const matchesSearch = searchTerm === ""
            || instance.entityType.toLowerCase().includes(searchTerm.toLowerCase())
            || instance.entityId.toString().includes(searchTerm)
            || instance.workflowName.toLowerCase().includes(searchTerm.toLowerCase())
            || instance.currentStateName.toLowerCase().includes(searchTerm.toLowerCase());

        return matchesStatus && matchesSearch;
    });

    if (loading) {
        return (
            <div className="d-flex align-items-center justify-content-center" style={{ minHeight: '60vh' }}>
                <div className="spinner-border text-primary" role="status">
                    <span className="visually-hidden">Loading...</span>
                </div>
            </div>
        );
    }

    return (
        <div className="workflows-page">
            {/* Header */}
            <div className="d-flex align-items-center justify-content-between mb-4">
                <div>
                    <h4 className="fw-semibold mb-1">Workflows</h4>
                    <p className="text-secondary small mb-0">Manage workflow definitions and instances</p>
                </div>
                <button className="btn btn-primary btn-sm" onClick={fetchData}>
                    <i className="bi bi-arrow-clockwise me-1"></i>
                    Refresh
                </button>
            </div>

            {error && (
                <div className="alert alert-danger alert-dismissible fade show" role="alert">
                    <i className="bi bi-exclamation-triangle me-2"></i>
                    {error}
                    <button type="button" className="btn-close" onClick={() => setError(null)}></button>
                </div>
            )}

            {/* Tabs */}
            <ul className="nav nav-tabs mb-4">
                <li className="nav-item">
                    <button
                        className={`nav-link ${activeTab === 'instances' ? 'active' : ''}`}
                        onClick={() => setActiveTab('instances')}
                    >
                        <i className="bi bi-activity me-2"></i>
                        Instances
                        <span className="badge bg-secondary ms-2">{instances.length}</span>
                    </button>
                </li>
                <li className="nav-item">
                    <button
                        className={`nav-link ${activeTab === 'definitions' ? 'active' : ''}`}
                        onClick={() => setActiveTab('definitions')}
                    >
                        <i className="bi bi-diagram-3 me-2"></i>
                        Definitions
                        <span className="badge bg-secondary ms-2">{definitions.length}</span>
                    </button>
                </li>
            </ul>

            {/* Instances Tab */}
            {activeTab === 'instances' && (
                <div className="card border-0 shadow-sm">
                    <div className="card-header bg-white py-3">
                        <div className="row g-3 align-items-center">
                            <div className="col-md-4">
                                <div className="input-group input-group-sm">
                                    <span className="input-group-text bg-white">
                                        <i className="bi bi-search"></i>
                                    </span>
                                    <input
                                        type="text"
                                        className="form-control"
                                        placeholder="Search instances..."
                                        value={searchTerm}
                                        onChange={(e) => setSearchTerm(e.target.value)}
                                    />
                                </div>
                            </div>
                            <div className="col-md-4">
                                <div className="btn-group btn-group-sm w-100">
                                    <button
                                        className={`btn ${statusFilter === 'all' ? 'btn-primary' : 'btn-outline-secondary'}`}
                                        onClick={() => setStatusFilter('all')}
                                    >
                                        All
                                    </button>
                                    <button
                                        className={`btn ${statusFilter === 'active' ? 'btn-primary' : 'btn-outline-secondary'}`}
                                        onClick={() => setStatusFilter('active')}
                                    >
                                        Active
                                    </button>
                                    <button
                                        className={`btn ${statusFilter === 'completed' ? 'btn-primary' : 'btn-outline-secondary'}`}
                                        onClick={() => setStatusFilter('completed')}
                                    >
                                        Completed
                                    </button>
                                </div>
                            </div>
                            <div className="col-md-4 text-end">
                                <span className="text-secondary small">{filteredInstances.length} instances</span>
                            </div>
                        </div>
                    </div>
                    <div className="card-body p-0">
                        {filteredInstances.length === 0 ? (
                            <div className="text-center py-5 text-secondary">
                                <i className="bi bi-inbox fs-1 d-block mb-2"></i>
                                <span>No workflow instances found</span>
                            </div>
                        ) : (
                            <div className="table-responsive">
                                <table className="table table-hover mb-0">
                                    <thead className="bg-light">
                                        <tr>
                                            <th className="border-0 ps-3" style={{ fontSize: '12px' }}>ID</th>
                                            <th className="border-0" style={{ fontSize: '12px' }}>Workflow</th>
                                            <th className="border-0" style={{ fontSize: '12px' }}>Entity</th>
                                            <th className="border-0" style={{ fontSize: '12px' }}>Current State</th>
                                            <th className="border-0" style={{ fontSize: '12px' }}>Created</th>
                                            <th className="border-0" style={{ fontSize: '12px' }}>Updated</th>
                                            <th className="border-0 pe-3" style={{ fontSize: '12px' }}>Actions</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {filteredInstances.map((instance) => (
                                            <tr key={instance.id}>
                                                <td className="ps-3">
                                                    <span className="text-secondary">#{instance.id}</span>
                                                </td>
                                                <td>
                                                    <span className="small fw-medium">{instance.workflowName}</span>
                                                </td>
                                                <td>
                                                    <span className="badge bg-light text-dark">{instance.entityType}</span>
                                                    <span className="ms-1 text-secondary">#{instance.entityId}</span>
                                                </td>
                                                <td>
                                                    <span
                                                        className="badge rounded-pill"
                                                        style={{
                                                            backgroundColor: getStateColor(instance.currentStateName),
                                                            fontSize: '11px'
                                                        }}
                                                    >
                                                        {instance.currentStateName}
                                                    </span>
                                                </td>
                                                <td>
                                                    <span className="small text-secondary">{formatTimeAgo(instance.createdAt)}</span>
                                                </td>
                                                <td>
                                                    <span className="small text-secondary">{formatTimeAgo(instance.updatedAt)}</span>
                                                </td>
                                                <td className="pe-3">
                                                    <Link
                                                        to={`/ops/workflows/${instance.id}`}
                                                        className="btn btn-sm btn-outline-primary"
                                                        style={{ fontSize: '11px' }}
                                                    >
                                                        <i className="bi bi-eye me-1"></i>
                                                        View
                                                    </Link>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </div>
                </div>
            )}

            {/* Definitions Tab */}
            {activeTab === 'definitions' && (
                <div className="row g-4">
                    {definitions.map((def) => (
                        <div key={def.id} className="col-md-6 col-lg-4">
                            <div className="card border-0 shadow-sm h-100">
                                <div className="card-header bg-white border-bottom py-3">
                                    <div className="d-flex align-items-center justify-content-between">
                                        <h6 className="fw-semibold mb-0">{def.name}</h6>
                                        <span className="badge bg-primary-subtle text-primary">{def.entityType}</span>
                                    </div>
                                </div>
                                <div className="card-body">
                                    <p className="text-secondary small mb-3">{def.description || 'No description available'}</p>

                                    <div className="mb-3">
                                        <div className="small text-secondary mb-2 fw-medium">States ({def.states?.length || 0})</div>
                                        <div className="d-flex flex-wrap gap-1">
                                            {def.states?.map((state) => (
                                                <span
                                                    key={state.id}
                                                    className="badge rounded-pill"
                                                    style={{
                                                        backgroundColor: state.colorCode || '#e9ecef',
                                                        color: state.colorCode ? '#fff' : '#495057',
                                                        fontSize: '10px'
                                                    }}
                                                    title={`${state.isInitial ? 'Initial' : ''} ${state.isTerminal ? 'Terminal' : ''}`}
                                                >
                                                    {state.isInitial && <i className="bi bi-play-fill me-1"></i>}
                                                    {state.stateName}
                                                    {state.isTerminal && <i className="bi bi-stop-fill ms-1"></i>}
                                                </span>
                                            ))}
                                        </div>
                                    </div>

                                    <div>
                                        <div className="small text-secondary mb-2 fw-medium">Transitions ({def.transitions?.length || 0})</div>
                                        <div className="small text-secondary">
                                            {def.transitions?.slice(0, 3).map((t, idx) => (
                                                <div key={t.id || idx} className="mb-1">
                                                    <i className="bi bi-arrow-right-short"></i>
                                                    {t.actionName}
                                                </div>
                                            ))}
                                            {def.transitions?.length > 3 && (
                                                <div className="text-secondary">+{def.transitions.length - 3} more</div>
                                            )}
                                        </div>
                                    </div>
                                </div>
                                <div className="card-footer bg-white border-top py-2">
                                    <div className="d-flex justify-content-between align-items-center">
                                        <small className="text-secondary">
                                            Active: {def.isActive ? <span className="text-success">Yes</span> : <span className="text-danger">No</span>}
                                        </small>
                                    </div>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default WorkflowsPage;
