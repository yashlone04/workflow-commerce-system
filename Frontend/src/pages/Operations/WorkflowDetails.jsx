import React, { useState, useEffect } from "react";
import { useParams, Link } from "react-router-dom";
import workflowService from "../../services/workflow.service";

const WorkflowDetails = () => {
    const { instanceId } = useParams();
    const [instance, setInstance] = useState(null);
    const [transitions, setTransitions] = useState([]);
    const [logs, setLogs] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [transitioning, setTransitioning] = useState(false);
    const [transitionComment, setTransitionComment] = useState("");
    const [selectedTransition, setSelectedTransition] = useState(null);

    useEffect(() => {
        if (instanceId) {
            fetchWorkflowData();
        }
    }, [instanceId]);

    const fetchWorkflowData = async () => {
        try {
            setLoading(true);
            setError(null);

            const [instanceRes, transitionsRes, logsRes] = await Promise.all([
                workflowService.getInstanceById(instanceId),
                workflowService.getAllowedTransitions(instanceId),
                workflowService.getInstanceLogs(instanceId)
            ]);

            setInstance(instanceRes.data?.data || instanceRes.data);
            setTransitions(transitionsRes.data?.data || transitionsRes.data || []);
            setLogs(logsRes.data?.data || logsRes.data || []);
        } catch (err) {
            console.error("Failed to fetch workflow instance:", err);
            setError("Failed to load workflow details");
        } finally {
            setLoading(false);
        }
    };

    const handleTransition = async (targetState) => {
        try {
            setTransitioning(true);
            await workflowService.performTransition(instanceId, targetState, transitionComment);
            setTransitionComment("");
            setSelectedTransition(null);
            await fetchWorkflowData();
        } catch (err) {
            console.error("Transition failed:", err);
            setError(err.response?.data?.message || "Transition failed");
        } finally {
            setTransitioning(false);
        }
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
            'PICKED_UP': '#198754',
            'RETURNED': '#dc3545'
        };
        return colors[state] || '#6c757d';
    };

    const getStateIcon = (state) => {
        const icons = {
            'CREATED': 'bi-plus-circle',
            'PAYMENT_PENDING': 'bi-hourglass-split',
            'PAID': 'bi-credit-card-2-front',
            'PROCESSING': 'bi-gear',
            'SHIPPED': 'bi-truck',
            'DELIVERED': 'bi-check-circle-fill',
            'CANCELLED': 'bi-x-circle',
            'REFUNDED': 'bi-arrow-counterclockwise',
            'INITIATED': 'bi-play-circle',
            'COMPLETED': 'bi-check-circle-fill',
            'FAILED': 'bi-exclamation-triangle',
            'IN_TRANSIT': 'bi-truck',
            'OUT_FOR_DELIVERY': 'bi-geo-alt',
            'PENDING': 'bi-clock',
            'PICKED_UP': 'bi-box-seam',
            'RETURNED': 'bi-arrow-return-left'
        };
        return icons[state] || 'bi-circle';
    };

    const getActionLabel = (fromState, toState) => {
        const safeToState = toState || 'UNKNOWN';
        const actions = {
            'CREATED_PAYMENT_PENDING': 'Order Placed',
            'PAYMENT_PENDING_PAID': 'Payment Completed',
            'PAID_PROCESSING': 'Processing Started',
            'PROCESSING_SHIPPED': 'Order Shipped',
            'SHIPPED_DELIVERED': 'Order Delivered',
            'CANCELLED': 'Order Cancelled',
            'REFUNDED': 'Payment Refunded'
        };
        const key = fromState ? `${fromState}_${safeToState}` : safeToState;
        return actions[key] || actions[safeToState] || `Moved to ${safeToState.replace(/_/g, ' ')}`;
    };

    const formatDateTime = (timestamp) => {
        if (!timestamp) return 'N/A';
        return new Date(timestamp).toLocaleString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
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

    if (!instance) {
        return (
            <div className="text-center py-5">
                <i className="bi bi-exclamation-circle fs-1 text-secondary mb-3 d-block"></i>
                <h5>Workflow Instance Not Found</h5>
                <p className="text-secondary">The requested workflow instance does not exist.</p>
                <Link to="/ops/workflows" className="btn btn-primary">Back to Workflows</Link>
            </div>
        );
    }

    return (
        <div className="workflow-details">
            {/* Header */}
            <div className="d-flex align-items-center justify-content-between mb-4">
                <div>
                    <nav aria-label="breadcrumb">
                        <ol className="breadcrumb small mb-2">
                            <li className="breadcrumb-item">
                                <Link to="/ops/workflows" className="text-decoration-none">Workflows</Link>
                            </li>
                            <li className="breadcrumb-item active">Instance #{instance.id}</li>
                        </ol>
                    </nav>
                    <h4 className="fw-semibold mb-1">
                        {instance.workflowName?.replace(/([A-Z])/g, ' $1').trim()}
                    </h4>
                    <p className="text-secondary small mb-0">
                        {instance.entityType} #{instance.entityId}
                    </p>
                </div>
                <button className="btn btn-outline-secondary btn-sm" onClick={fetchWorkflowData}>
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

            <div className="row g-4">
                {/* Current State Card */}
                <div className="col-lg-4">
                    <div className="card border-0 shadow-sm h-100">
                        <div className="card-body text-center py-5">
                            <div className="mb-4">
                                <div
                                    className="d-inline-flex align-items-center justify-content-center rounded-circle mb-3"
                                    style={{
                                        width: '80px',
                                        height: '80px',
                                        backgroundColor: getStateColor(instance.currentStateName) + '20'
                                    }}
                                >
                                    <i
                                        className="bi bi-circle-fill fs-1"
                                        style={{ color: getStateColor(instance.currentStateName) }}
                                    ></i>
                                </div>
                                <h5 className="fw-semibold mb-2">Current State</h5>
                                <span
                                    className="badge rounded-pill fs-6 px-4 py-2"
                                    style={{ backgroundColor: getStateColor(instance.currentStateName) }}
                                >
                                    {instance.currentStateName}
                                </span>
                            </div>
                            <hr />
                            <div className="text-start">
                                <div className="row g-3 small">
                                    <div className="col-6">
                                        <div className="text-secondary">Instance ID</div>
                                        <div className="fw-medium">#{instance.id}</div>
                                    </div>
                                    <div className="col-6">
                                        <div className="text-secondary">Entity</div>
                                        <div className="fw-medium">{instance.entityType} #{instance.entityId}</div>
                                    </div>
                                    <div className="col-6">
                                        <div className="text-secondary">Created</div>
                                        <div className="fw-medium">{formatDateTime(instance.createdAt)}</div>
                                    </div>
                                    <div className="col-6">
                                        <div className="text-secondary">Updated</div>
                                        <div className="fw-medium">{formatDateTime(instance.updatedAt)}</div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Available Transitions */}
                <div className="col-lg-8">
                    <div className="card border-0 shadow-sm h-100">
                        <div className="card-header bg-white border-bottom py-3">
                            <h6 className="fw-semibold mb-0">
                                <i className="bi bi-arrow-right-circle me-2 text-primary"></i>
                                Available Transitions
                            </h6>
                        </div>
                        <div className="card-body">
                            {transitions.length === 0 ? (
                                <div className="text-center py-5 text-secondary">
                                    <i className="bi bi-check-circle fs-1 text-success d-block mb-2"></i>
                                    <span>No transitions available. This workflow may be in a terminal state.</span>
                                </div>
                            ) : (
                                <>
                                    <div className="row g-3 mb-4">
                                        {transitions.map((transition) => (
                                            <div key={transition.id} className="col-md-6">
                                                <div
                                                    className={`card h-100 border cursor-pointer ${selectedTransition?.id === transition.id ? 'border-primary' : ''}`}
                                                    onClick={() => setSelectedTransition(transition)}
                                                    style={{ cursor: 'pointer' }}
                                                >
                                                    <div className="card-body py-3">
                                                        <div className="d-flex align-items-center justify-content-between mb-2">
                                                            <span className="fw-medium">{transition.actionName}</span>
                                                            <i className="bi bi-arrow-right text-primary"></i>
                                                        </div>
                                                        <div className="d-flex align-items-center gap-2">
                                                            <span
                                                                className="badge rounded-pill"
                                                                style={{ backgroundColor: getStateColor(instance.currentStateName), fontSize: '10px' }}
                                                            >
                                                                {instance.currentStateName}
                                                            </span>
                                                            <i className="bi bi-arrow-right-short text-secondary"></i>
                                                            <span
                                                                className="badge rounded-pill"
                                                                style={{ backgroundColor: getStateColor(transition.toStateName), fontSize: '10px' }}
                                                            >
                                                                {transition.toStateName}
                                                            </span>
                                                        </div>
                                                        {transition.requiresComment && (
                                                            <div className="mt-2">
                                                                <small className="text-warning">
                                                                    <i className="bi bi-exclamation-circle me-1"></i>
                                                                    Comment required
                                                                </small>
                                                            </div>
                                                        )}
                                                    </div>
                                                </div>
                                            </div>
                                        ))}
                                    </div>

                                    {selectedTransition && (
                                        <div className="border rounded-3 p-3 bg-light">
                                            <h6 className="fw-semibold mb-3">Perform Transition: {selectedTransition.actionName}</h6>
                                            <div className="mb-3">
                                                <label className="form-label small text-secondary">
                                                    Comment {selectedTransition.requiresComment ? <span className="text-danger">*</span> : '(optional)'}
                                                </label>
                                                <textarea
                                                    className="form-control"
                                                    rows="2"
                                                    value={transitionComment}
                                                    onChange={(e) => setTransitionComment(e.target.value)}
                                                    placeholder="Add a comment for this transition..."
                                                />
                                            </div>
                                            <div className="d-flex gap-2">
                                                <button
                                                    className="btn btn-primary"
                                                    onClick={() => handleTransition(selectedTransition.toStateName)}
                                                    disabled={transitioning || (selectedTransition.requiresComment && !transitionComment.trim())}
                                                >
                                                    {transitioning ? (
                                                        <>
                                                            <span className="spinner-border spinner-border-sm me-2"></span>
                                                            Processing...
                                                        </>
                                                    ) : (
                                                        <>
                                                            <i className="bi bi-check me-1"></i>
                                                            Confirm Transition
                                                        </>
                                                    )}
                                                </button>
                                                <button
                                                    className="btn btn-outline-secondary"
                                                    onClick={() => {
                                                        setSelectedTransition(null);
                                                        setTransitionComment("");
                                                    }}
                                                >
                                                    Cancel
                                                </button>
                                            </div>
                                        </div>
                                    )}
                                </>
                            )}
                        </div>
                    </div>
                </div>
            </div>

            {/* Workflow Timeline - Modern Design */}
            <div className="card border-0 shadow-sm mt-4">
                <div className="card-header bg-gradient" style={{ background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' }}>
                    <div className="d-flex align-items-center justify-content-between py-2">
                        <h6 className="fw-semibold mb-0 text-white">
                            <i className="bi bi-diagram-3 me-2"></i>
                            Workflow Journey
                        </h6>
                        <span className="badge bg-white text-dark">
                            {logs.length} {logs.length === 1 ? 'transition' : 'transitions'}
                        </span>
                    </div>
                </div>
                <div className="card-body p-4" style={{ background: '#fafbfc' }}>
                    {logs.length === 0 ? (
                        <div className="text-center py-5">
                            <div className="mb-3" style={{ fontSize: '60px', opacity: 0.3 }}>
                                <i className="bi bi-hourglass-split"></i>
                            </div>
                            <h6 className="text-secondary">No Journey Started</h6>
                            <p className="text-muted small mb-0">Transitions will appear here as the workflow progresses</p>
                        </div>
                    ) : (
                        <div className="workflow-timeline position-relative">
                            {/* Timeline vertical line */}
                            <div
                                className="position-absolute"
                                style={{
                                    left: '24px',
                                    top: '30px',
                                    bottom: '30px',
                                    width: '3px',
                                    background: 'linear-gradient(180deg, #667eea 0%, #764ba2 50%, #198754 100%)',
                                    borderRadius: '2px'
                                }}
                            ></div>

                            {logs.map((log, idx) => (
                                <div
                                    key={log.id || idx}
                                    className="timeline-item d-flex mb-4 position-relative"
                                    style={{
                                        animation: 'fadeInUp 0.3s ease-out',
                                        animationDelay: `${idx * 0.1}s`,
                                        animationFillMode: 'both'
                                    }}
                                >
                                    {/* Timeline Node */}
                                    <div className="timeline-node position-relative" style={{ minWidth: '50px', zIndex: 1 }}>
                                        <div
                                            className="rounded-circle d-flex align-items-center justify-content-center shadow-sm"
                                            style={{
                                                width: '50px',
                                                height: '50px',
                                                backgroundColor: '#fff',
                                                border: `3px solid ${getStateColor(log.toState || 'UNKNOWN')}`,
                                                transition: 'transform 0.2s ease'
                                            }}
                                        >
                                            <i
                                                className={`bi ${getStateIcon(log.toState || 'UNKNOWN')}`}
                                                style={{ color: getStateColor(log.toState || 'UNKNOWN'), fontSize: '20px' }}
                                            ></i>
                                        </div>
                                        {idx === 0 && (
                                            <div
                                                className="position-absolute text-nowrap"
                                                style={{
                                                    top: '-8px',
                                                    left: '55px',
                                                    fontSize: '10px',
                                                    background: getStateColor(log.toState || 'UNKNOWN'),
                                                    color: '#fff',
                                                    padding: '2px 8px',
                                                    borderRadius: '10px'
                                                }}
                                            >
                                                Latest
                                            </div>
                                        )}
                                    </div>

                                    {/* Timeline Content */}
                                    <div
                                        className="timeline-content flex-grow-1 ms-3 p-3 rounded-3 bg-white shadow-sm"
                                        style={{
                                            border: '1px solid #e9ecef',
                                            transition: 'box-shadow 0.2s ease'
                                        }}
                                    >
                                        {/* Action Title */}
                                        <div className="d-flex align-items-start justify-content-between mb-2">
                                            <div>
                                                <h6 className="fw-bold mb-1" style={{ color: getStateColor(log.toState || 'UNKNOWN') }}>
                                                    {getActionLabel(log.fromState, log.toState)}
                                                </h6>
                                                <div className="d-flex align-items-center gap-2 flex-wrap">
                                                    {log.fromState && (
                                                        <>
                                                            <span
                                                                className="badge"
                                                                style={{
                                                                    backgroundColor: getStateColor(log.fromState) + '15',
                                                                    color: getStateColor(log.fromState),
                                                                    border: `1px solid ${getStateColor(log.fromState)}30`,
                                                                    fontSize: '11px',
                                                                    fontWeight: 500
                                                                }}
                                                            >
                                                                {(log.fromState || '').replace(/_/g, ' ')}
                                                            </span>
                                                            <i className="bi bi-arrow-right text-secondary" style={{ fontSize: '12px' }}></i>
                                                        </>
                                                    )}
                                                    <span
                                                        className="badge"
                                                        style={{
                                                            backgroundColor: getStateColor(log.toState || 'UNKNOWN'),
                                                            color: '#fff',
                                                            fontSize: '11px',
                                                            fontWeight: 600
                                                        }}
                                                    >
                                                        {(log.toState || 'Unknown').replace(/_/g, ' ')}
                                                    </span>
                                                </div>
                                            </div>
                                            <div className="text-end">
                                                <div className="small text-secondary" style={{ whiteSpace: 'nowrap' }}>
                                                    <i className="bi bi-clock me-1"></i>
                                                    {formatDateTime(log.performedAt)}
                                                </div>
                                            </div>
                                        </div>

                                        {/* Performer Info */}
                                        <div className="d-flex align-items-center mt-2 pt-2 border-top">
                                            <div
                                                className="rounded-circle d-flex align-items-center justify-content-center me-2"
                                                style={{
                                                    width: '28px',
                                                    height: '28px',
                                                    backgroundColor: '#667eea15',
                                                    color: '#667eea',
                                                    fontSize: '12px',
                                                    fontWeight: 600
                                                }}
                                            >
                                                {(log.performedBy || 'S').charAt(0).toUpperCase()}
                                            </div>
                                            <div>
                                                <span className="small fw-medium">{log.performedBy || 'System'}</span>
                                                <span
                                                    className="badge ms-2"
                                                    style={{
                                                        backgroundColor: log.performedRole?.includes('ADMIN') ? '#dc354520' : '#0d6efd20',
                                                        color: log.performedRole?.includes('ADMIN') ? '#dc3545' : '#0d6efd',
                                                        fontSize: '9px',
                                                        fontWeight: 500
                                                    }}
                                                >
                                                    {(log.performedRole || 'SYSTEM').replace('ROLE_', '')}
                                                </span>
                                            </div>
                                        </div>

                                        {/* Comment if exists */}
                                        {log.comment && (
                                            <div
                                                className="mt-2 p-2 rounded"
                                                style={{ backgroundColor: '#f8f9fa', borderLeft: '3px solid #667eea' }}
                                            >
                                                <i className="bi bi-chat-quote me-2 text-secondary" style={{ fontSize: '12px' }}></i>
                                                <span className="small text-secondary fst-italic">"{log.comment}"</span>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </div>

            {/* Timeline Animation Styles */}
            <style>{`
                @keyframes fadeInUp {
                    from {
                        opacity: 0;
                        transform: translateY(20px);
                    }
                    to {
                        opacity: 1;
                        transform: translateY(0);
                    }
                }
                .timeline-item:hover .timeline-content {
                    box-shadow: 0 4px 15px rgba(0,0,0,0.1) !important;
                }
                .timeline-item:hover .timeline-node > div {
                    transform: scale(1.1);
                }
            `}</style>
        </div>
    );
};

export default WorkflowDetails;
