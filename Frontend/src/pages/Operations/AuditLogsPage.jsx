import React, { useState, useEffect } from "react";
import workflowService from "../../services/workflow.service";

const AuditLogsPage = () => {
    const [logs, setLogs] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [searchTerm, setSearchTerm] = useState("");
    const [limit, setLimit] = useState(100);

    useEffect(() => {
        fetchLogs();
    }, [limit]);

    const fetchLogs = async () => {
        try {
            setLoading(true);
            const response = await workflowService.getRecentLogs(limit);
            setLogs(response.data?.data || response.data || []);
        } catch (err) {
            console.error("Failed to fetch logs:", err);
            setError("Failed to load audit logs");
        } finally {
            setLoading(false);
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
            'PICKED_UP': '#198754'
        };
        return colors[state] || '#6c757d';
    };

    const formatDateTime = (timestamp) => {
        return new Date(timestamp).toLocaleString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });
    };

    const filteredLogs = logs.filter(log => {
        if (searchTerm === "") return true;
        const term = searchTerm.toLowerCase();
        return (
            log.performedBy?.toLowerCase().includes(term) ||
            log.toState?.toLowerCase().includes(term) ||
            log.fromState?.toLowerCase().includes(term) ||
            log.comment?.toLowerCase().includes(term) ||
            log.performedRole?.toLowerCase().includes(term)
        );
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
        <div className="audit-logs-page">
            {/* Header */}
            <div className="d-flex align-items-center justify-content-between mb-4">
                <div>
                    <h4 className="fw-semibold mb-1">Audit Logs</h4>
                    <p className="text-secondary small mb-0">Track all workflow transitions and system activity</p>
                </div>
                <button className="btn btn-primary btn-sm" onClick={fetchLogs}>
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

            {/* Filters */}
            <div className="card border-0 shadow-sm mb-4">
                <div className="card-body py-3">
                    <div className="row g-3 align-items-center">
                        <div className="col-md-5">
                            <div className="input-group input-group-sm">
                                <span className="input-group-text bg-white">
                                    <i className="bi bi-search"></i>
                                </span>
                                <input
                                    type="text"
                                    className="form-control"
                                    placeholder="Search by user, state, comment..."
                                    value={searchTerm}
                                    onChange={(e) => setSearchTerm(e.target.value)}
                                />
                            </div>
                        </div>
                        <div className="col-md-3">
                            <select
                                className="form-select form-select-sm"
                                value={limit}
                                onChange={(e) => setLimit(parseInt(e.target.value))}
                            >
                                <option value="50">Last 50 logs</option>
                                <option value="100">Last 100 logs</option>
                                <option value="250">Last 250 logs</option>
                                <option value="500">Last 500 logs</option>
                            </select>
                        </div>
                        <div className="col-md-4 text-end">
                            <span className="text-secondary small">
                                Showing {filteredLogs.length} of {logs.length} logs
                            </span>
                        </div>
                    </div>
                </div>
            </div>

            {/* Logs Table */}
            <div className="card border-0 shadow-sm">
                <div className="card-body p-0">
                    {filteredLogs.length === 0 ? (
                        <div className="text-center py-5 text-secondary">
                            <i className="bi bi-clock-history fs-1 d-block mb-2"></i>
                            <span>No audit logs found</span>
                        </div>
                    ) : (
                        <div className="table-responsive">
                            <table className="table table-hover mb-0">
                                <thead className="bg-light">
                                    <tr>
                                        <th className="border-0 ps-3" style={{ fontSize: '12px' }}>Timestamp</th>
                                        <th className="border-0" style={{ fontSize: '12px' }}>Performed By</th>
                                        <th className="border-0" style={{ fontSize: '12px' }}>Role</th>
                                        <th className="border-0" style={{ fontSize: '12px' }}>Transition</th>
                                        <th className="border-0" style={{ fontSize: '12px' }}>Comment</th>
                                        <th className="border-0 pe-3" style={{ fontSize: '12px' }}>Instance</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {filteredLogs.map((log, idx) => (
                                        <tr key={log.id || idx}>
                                            <td className="ps-3">
                                                <span className="small text-nowrap">{formatDateTime(log.performedAt)}</span>
                                            </td>
                                            <td>
                                                <span className="fw-medium">{log.performedBy}</span>
                                            </td>
                                            <td>
                                                <span className="badge bg-light text-secondary" style={{ fontSize: '10px' }}>
                                                    {log.performedRole}
                                                </span>
                                            </td>
                                            <td>
                                                <div className="d-flex align-items-center gap-1">
                                                    {log.fromState && (
                                                        <>
                                                            <span
                                                                className="badge rounded-pill"
                                                                style={{ backgroundColor: getStateColor(log.fromState), fontSize: '10px' }}
                                                            >
                                                                {log.fromState}
                                                            </span>
                                                            <i className="bi bi-arrow-right-short text-secondary"></i>
                                                        </>
                                                    )}
                                                    <span
                                                        className="badge rounded-pill"
                                                        style={{ backgroundColor: getStateColor(log.toState), fontSize: '10px' }}
                                                    >
                                                        {log.toState}
                                                    </span>
                                                </div>
                                            </td>
                                            <td>
                                                {log.comment ? (
                                                    <span className="small text-secondary fst-italic" title={log.comment}>
                                                        {log.comment.length > 40 ? log.comment.substring(0, 40) + '...' : log.comment}
                                                    </span>
                                                ) : (
                                                    <span className="small text-secondary">—</span>
                                                )}
                                            </td>
                                            <td className="pe-3">
                                                {log.workflowInstanceId && (
                                                    <a
                                                        href={`/ops/workflows/${log.workflowInstanceId}`}
                                                        className="btn btn-sm btn-link p-0"
                                                        style={{ fontSize: '11px' }}
                                                    >
                                                        #{log.workflowInstanceId}
                                                    </a>
                                                )}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            </div>

            {/* Summary Stats */}
            {logs.length > 0 && (
                <div className="row g-3 mt-4">
                    <div className="col-md-3">
                        <div className="card border-0 shadow-sm">
                            <div className="card-body text-center py-4">
                                <div className="fs-3 fw-bold text-primary">{logs.length}</div>
                                <div className="small text-secondary">Total Logs</div>
                            </div>
                        </div>
                    </div>
                    <div className="col-md-3">
                        <div className="card border-0 shadow-sm">
                            <div className="card-body text-center py-4">
                                <div className="fs-3 fw-bold text-success">
                                    {new Set(logs.map(l => l.performedBy)).size}
                                </div>
                                <div className="small text-secondary">Unique Users</div>
                            </div>
                        </div>
                    </div>
                    <div className="col-md-3">
                        <div className="card border-0 shadow-sm">
                            <div className="card-body text-center py-4">
                                <div className="fs-3 fw-bold text-info">
                                    {new Set(logs.map(l => l.toState)).size}
                                </div>
                                <div className="small text-secondary">States Reached</div>
                            </div>
                        </div>
                    </div>
                    <div className="col-md-3">
                        <div className="card border-0 shadow-sm">
                            <div className="card-body text-center py-4">
                                <div className="fs-3 fw-bold text-warning">
                                    {logs.filter(l => l.comment).length}
                                </div>
                                <div className="small text-secondary">With Comments</div>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default AuditLogsPage;
