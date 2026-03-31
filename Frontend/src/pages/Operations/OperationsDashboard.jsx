import React, { useState, useEffect } from "react";
import workflowService from "../../services/workflow.service";
import adminOrderService from "../../services/adminOrder.service";

const OperationsDashboard = () => {
    const [workflowStats, setWorkflowStats] = useState([]);
    const [recentLogs, setRecentLogs] = useState([]);
    const [activeInstances, setActiveInstances] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        fetchDashboardData();
    }, []);

    const fetchDashboardData = async () => {
        try {
            setLoading(true);
            setError(null);

            const [definitionsRes, logsRes, instancesRes] = await Promise.all([
                workflowService.getAllDefinitions(),
                workflowService.getRecentLogs(10),
                workflowService.getAllInstances()
            ]);

            const definitions = definitionsRes.data?.data || definitionsRes.data || [];

            // Fetch stats for each workflow
            const statsPromises = definitions.map(async (def) => {
                try {
                    const statsRes = await workflowService.getWorkflowStats(def.id);
                    return { ...def, stats: statsRes.data?.data || statsRes.data };
                } catch (e) {
                    return { ...def, stats: null };
                }
            });

            const workflowsWithStats = await Promise.all(statsPromises);
            setWorkflowStats(workflowsWithStats);
            setRecentLogs(logsRes.data?.data || logsRes.data || []);

            const instances = instancesRes.data?.data || instancesRes.data || [];
            setActiveInstances(instances.filter(i => !isTerminalState(i.currentStateName)));
        } catch (err) {
            console.error("Failed to fetch dashboard data:", err);
            setError("Failed to load dashboard data");
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
        <div className="ops-dashboard">
            {/* Header */}
            <div className="d-flex align-items-center justify-content-between mb-4">
                <div>
                    <h4 className="fw-semibold mb-1">Operations Dashboard</h4>
                    <p className="text-secondary small mb-0">Workflow orchestration and operations monitoring</p>
                </div>
                <button className="btn btn-primary btn-sm" onClick={fetchDashboardData}>
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

            {/* Workflow Stats Cards */}
            <div className="row g-3 mb-4">
                {workflowStats.map((workflow) => (
                    <div key={workflow.id} className="col-md-4">
                        <div className="card border-0 shadow-sm h-100">
                            <div className="card-body">
                                <div className="d-flex align-items-center justify-content-between mb-3">
                                    <h6 className="fw-semibold mb-0">{workflow.name?.replace('Workflow', ' Workflow')}</h6>
                                    <span className="badge bg-light text-secondary">{workflow.entityType}</span>
                                </div>
                                {workflow.stats ? (
                                    <div className="row g-2">
                                        <div className="col-4 text-center">
                                            <div className="fs-4 fw-bold text-primary">{workflow.stats.totalInstances || 0}</div>
                                            <div className="small text-secondary">Total</div>
                                        </div>
                                        <div className="col-4 text-center">
                                            <div className="fs-4 fw-bold text-success">{workflow.stats.activeInstances || 0}</div>
                                            <div className="small text-secondary">Active</div>
                                        </div>
                                        <div className="col-4 text-center">
                                            <div className="fs-4 fw-bold text-secondary">{workflow.stats.completedInstances || 0}</div>
                                            <div className="small text-secondary">Done</div>
                                        </div>
                                    </div>
                                ) : (
                                    <div className="text-secondary small">No statistics available</div>
                                )}
                                <hr className="my-3" />
                                <div className="d-flex flex-wrap gap-1">
                                    {workflow.states?.slice(0, 5).map((state) => (
                                        <span
                                            key={state.id}
                                            className="badge rounded-pill"
                                            style={{
                                                backgroundColor: state.colorCode || '#e9ecef',
                                                color: state.colorCode ? '#fff' : '#495057',
                                                fontSize: '10px'
                                            }}
                                        >
                                            {state.stateName}
                                        </span>
                                    ))}
                                    {workflow.states?.length > 5 && (
                                        <span className="badge bg-light text-secondary rounded-pill" style={{ fontSize: '10px' }}>
                                            +{workflow.states.length - 5}
                                        </span>
                                    )}
                                </div>
                            </div>
                        </div>
                    </div>
                ))}
            </div>

            <div className="row g-4">
                {/* Active Workflow Instances */}
                <div className="col-lg-7">
                    <div className="card border-0 shadow-sm">
                        <div className="card-header bg-white border-bottom py-3">
                            <div className="d-flex align-items-center justify-content-between">
                                <h6 className="fw-semibold mb-0">
                                    <i className="bi bi-activity me-2 text-primary"></i>
                                    Active Workflow Instances
                                </h6>
                                <span className="badge bg-primary">{activeInstances.length}</span>
                            </div>
                        </div>
                        <div className="card-body p-0">
                            {activeInstances.length === 0 ? (
                                <div className="text-center py-5 text-secondary">
                                    <i className="bi bi-inbox fs-1 d-block mb-2"></i>
                                    <span>No active workflow instances</span>
                                </div>
                            ) : (
                                <div className="table-responsive">
                                    <table className="table table-hover mb-0">
                                        <thead className="bg-light">
                                            <tr>
                                                <th className="border-0 ps-3" style={{ fontSize: '12px' }}>Entity</th>
                                                <th className="border-0" style={{ fontSize: '12px' }}>Workflow</th>
                                                <th className="border-0" style={{ fontSize: '12px' }}>Current State</th>
                                                <th className="border-0" style={{ fontSize: '12px' }}>Started</th>
                                                <th className="border-0 pe-3" style={{ fontSize: '12px' }}>Actions</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {activeInstances.slice(0, 10).map((instance) => (
                                                <tr key={instance.id}>
                                                    <td className="ps-3">
                                                        <span className="badge bg-light text-dark">{instance.entityType}</span>
                                                        <span className="ms-1 text-secondary">#{instance.entityId}</span>
                                                    </td>
                                                    <td>
                                                        <span className="small">{instance.workflowName}</span>
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
                                                    <td className="pe-3">
                                                        <a
                                                            href={`/ops/workflows/${instance.id}`}
                                                            className="btn btn-sm btn-outline-primary"
                                                            style={{ fontSize: '11px' }}
                                                        >
                                                            View
                                                        </a>
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </div>
                    </div>
                </div>

                {/* Recent Activity */}
                <div className="col-lg-5">
                    <div className="card border-0 shadow-sm">
                        <div className="card-header bg-white border-bottom py-3">
                            <h6 className="fw-semibold mb-0">
                                <i className="bi bi-clock-history me-2 text-secondary"></i>
                                Recent Activity
                            </h6>
                        </div>
                        <div className="card-body p-0">
                            {recentLogs.length === 0 ? (
                                <div className="text-center py-5 text-secondary">
                                    <i className="bi bi-clock fs-1 d-block mb-2"></i>
                                    <span>No recent activity</span>
                                </div>
                            ) : (
                                <div className="activity-feed">
                                    {recentLogs.map((log, idx) => (
                                        <div
                                            key={log.id || idx}
                                            className="activity-item d-flex px-3 py-3 border-bottom"
                                        >
                                            <div className="activity-icon me-3">
                                                <div
                                                    className="rounded-circle d-flex align-items-center justify-content-center"
                                                    style={{
                                                        width: '32px',
                                                        height: '32px',
                                                        backgroundColor: getStateColor(log.toState) + '20'
                                                    }}
                                                >
                                                    <i
                                                        className="bi bi-arrow-right"
                                                        style={{ color: getStateColor(log.toState), fontSize: '12px' }}
                                                    ></i>
                                                </div>
                                            </div>
                                            <div className="activity-content flex-grow-1">
                                                <div className="small">
                                                    <span className="fw-medium">{log.performedBy}</span>
                                                    <span className="text-secondary"> transitioned to </span>
                                                    <span
                                                        className="badge rounded-pill"
                                                        style={{
                                                            backgroundColor: getStateColor(log.toState),
                                                            fontSize: '10px'
                                                        }}
                                                    >
                                                        {log.toState}
                                                    </span>
                                                </div>
                                                {log.comment && (
                                                    <div className="text-secondary small mt-1" style={{ fontSize: '11px' }}>
                                                        "{log.comment}"
                                                    </div>
                                                )}
                                                <div className="text-secondary mt-1" style={{ fontSize: '11px' }}>
                                                    {formatTimeAgo(log.performedAt)}
                                                </div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            </div>

            {/* Quick Actions */}
            <div className="card border-0 shadow-sm mt-4">
                <div className="card-header bg-white border-bottom py-3">
                    <h6 className="fw-semibold mb-0">
                        <i className="bi bi-lightning me-2 text-warning"></i>
                        Quick Actions
                    </h6>
                </div>
                <div className="card-body">
                    <div className="row g-3">
                        <div className="col-md-3">
                            <a href="/ops/orders" className="card border h-100 text-decoration-none">
                                <div className="card-body text-center py-4">
                                    <i className="bi bi-receipt fs-3 text-primary mb-2 d-block"></i>
                                    <div className="fw-medium">Manage Orders</div>
                                    <div className="small text-secondary">Process and track orders</div>
                                </div>
                            </a>
                        </div>
                        <div className="col-md-3">
                            <a href="/ops/shipping" className="card border h-100 text-decoration-none">
                                <div className="card-body text-center py-4">
                                    <i className="bi bi-truck fs-3 text-success mb-2 d-block"></i>
                                    <div className="fw-medium">Shipping Queue</div>
                                    <div className="small text-secondary">Track shipments</div>
                                </div>
                            </a>
                        </div>
                        <div className="col-md-3">
                            <a href="/ops/payments" className="card border h-100 text-decoration-none">
                                <div className="card-body text-center py-4">
                                    <i className="bi bi-credit-card fs-3 text-info mb-2 d-block"></i>
                                    <div className="fw-medium">Payment Records</div>
                                    <div className="small text-secondary">Review payments</div>
                                </div>
                            </a>
                        </div>
                        <div className="col-md-3">
                            <a href="/ops/logs" className="card border h-100 text-decoration-none">
                                <div className="card-body text-center py-4">
                                    <i className="bi bi-clock-history fs-3 text-secondary mb-2 d-block"></i>
                                    <div className="fw-medium">Audit Logs</div>
                                    <div className="small text-secondary">View all activity</div>
                                </div>
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default OperationsDashboard;
