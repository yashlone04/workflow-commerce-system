import React, { useState, useEffect, useRef } from "react";
import { Link } from "react-router-dom";
import adminOrderService from "../../services/adminOrder.service";
import workflowService from "../../services/workflow.service";

const OpsOrdersPage = () => {
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [statusFilter, setStatusFilter] = useState("all");
    const [searchTerm, setSearchTerm] = useState("");
    const [selectedOrder, setSelectedOrder] = useState(null);
    const [orderWorkflow, setOrderWorkflow] = useState(null);
    const [transitions, setTransitions] = useState([]);
    const [transitionComment, setTransitionComment] = useState("");
    const [transitioning, setTransitioning] = useState(false);
    const [creatingWorkflow, setCreatingWorkflow] = useState(false);
    const [migrating, setMigrating] = useState(false);
    const hasFetched = useRef(false);

    useEffect(() => {
        if (!hasFetched.current) {
            hasFetched.current = true;
            fetchOrders();
        }
    }, []);

    const fetchOrders = async () => {
        try {
            setLoading(true);
            const response = await adminOrderService.getAllOrders();
            setOrders(Array.isArray(response.data) ? response.data : []);
        } catch (err) {
            console.error("Failed to fetch orders:", err);
            setError("Failed to load orders");
        } finally {
            setLoading(false);
        }
    };

    const fetchOrderWorkflow = async (orderId) => {
        try {
            const instanceRes = await workflowService.getOrderWorkflowInstance(orderId);
            const instance = instanceRes.data?.data || instanceRes.data;
            setOrderWorkflow(instance);

            if (instance?.id) {
                const transitionsRes = await workflowService.getAllowedTransitions(instance.id);
                setTransitions(transitionsRes.data?.data || transitionsRes.data || []);
            }
        } catch (err) {
            // 404 is expected when order doesn't have a workflow instance yet
            if (err.response?.status !== 404) {
                console.error("Failed to fetch order workflow:", err);
            }
            setOrderWorkflow(null);
            setTransitions([]);
        }
    };

    const handleSelectOrder = async (order) => {
        setSelectedOrder(order);
        setOrderWorkflow(null);
        setTransitions([]);
        await fetchOrderWorkflow(order.orderId);
    };

    const handleTransition = async (targetState) => {
        if (!orderWorkflow?.id) return;

        try {
            setTransitioning(true);
            await workflowService.performTransition(orderWorkflow.id, targetState, transitionComment);
            setTransitionComment("");

            // Refresh
            await fetchOrders();
            await fetchOrderWorkflow(selectedOrder.orderId);
        } catch (err) {
            console.error("Transition failed:", err);
            setError(err.response?.data?.message || "Transition failed");
        } finally {
            setTransitioning(false);
        }
    };

    const handleLegacyStatusUpdate = async (orderId, newStatus) => {
        try {
            await adminOrderService.updateOrderStatus(orderId, newStatus);
            await fetchOrders();
            // Don't refetch workflow - legacy mode means no workflow exists
        } catch (err) {
            setError(err.response?.data?.message || "Failed to update order status");
        }
    };

    const handleCreateWorkflow = async (orderId) => {
        try {
            setCreatingWorkflow(true);
            await workflowService.createInstance("OrderLifecycleWorkflow", "ORDER", orderId);
            // Refresh workflow data
            await fetchOrderWorkflow(orderId);
            setError(null);
        } catch (err) {
            console.error("Failed to create workflow:", err);
            setError(err.response?.data?.message || "Failed to create workflow instance");
        } finally {
            setCreatingWorkflow(false);
        }
    };

    const handleMigrateAll = async () => {
        try {
            setMigrating(true);
            const response = await workflowService.migrateAllOrders();
            const data = response.data;
            setError(null);
            alert(`Migration complete!\nMigrated: ${data.totalMigrated} orders\nFailed: ${data.totalFailed} orders`);
            fetchOrders();
            if (selectedOrder) {
                fetchOrderWorkflow(selectedOrder.orderId);
            }
        } catch (err) {
            console.error("Failed to migrate orders:", err);
            setError(err.response?.data?.message || "Failed to migrate orders");
        } finally {
            setMigrating(false);
        }
    };

    const getStatusColor = (status) => {
        const colors = {
            'PENDING': '#fd7e14',
            'CONFIRMED': '#0d6efd',
            'PROCESSING': '#0d6efd',
            'SHIPPED': '#6f42c1',
            'DELIVERED': '#198754',
            'CANCELLED': '#dc3545',
            'CREATED': '#6c757d',
            'PAYMENT_PENDING': '#fd7e14',
            'PAID': '#198754',
            'REFUNDED': '#ffc107',
        };
        return colors[status] || '#6c757d';
    };

    const formatDate = (timestamp) => {
        return new Date(timestamp).toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric'
        });
    };

    const formatCurrency = (amount) => {
        return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'USD'
        }).format(amount || 0);
    };

    const filteredOrders = orders.filter(order => {
        const matchesStatus = statusFilter === "all" || order.orderStatus === statusFilter;
        const matchesSearch = searchTerm === ""
            || order.orderId?.toString().includes(searchTerm)
            || order.customerEmail?.toLowerCase().includes(searchTerm.toLowerCase())
            || order.shippingAddress?.toLowerCase().includes(searchTerm.toLowerCase());
        return matchesStatus && matchesSearch;
    });

    const statusOptions = [...new Set(orders.map(o => o.orderStatus))].filter(Boolean);

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
        <div className="ops-orders-page">
            {/* Header */}
            <div className="d-flex align-items-center justify-content-between mb-4">
                <div>
                    <h4 className="fw-semibold mb-1">Order Operations</h4>
                    <p className="text-secondary small mb-0">Manage orders and workflow transitions</p>
                </div>
                <div className="d-flex gap-2">
                    <button
                        className="btn btn-warning btn-sm"
                        onClick={handleMigrateAll}
                        disabled={migrating}
                    >
                        {migrating ? (
                            <>
                                <span className="spinner-border spinner-border-sm me-1"></span>
                                Migrating...
                            </>
                        ) : (
                            <>
                                <i className="bi bi-arrow-repeat me-1"></i>
                                Migrate All to Workflow
                            </>
                        )}
                    </button>
                    <button className="btn btn-primary btn-sm" onClick={fetchOrders}>
                        <i className="bi bi-arrow-clockwise me-1"></i>
                        Refresh
                    </button>
                </div>
            </div>

            {error && (
                <div className="alert alert-danger alert-dismissible fade show" role="alert">
                    <i className="bi bi-exclamation-triangle me-2"></i>
                    {error}
                    <button type="button" className="btn-close" onClick={() => setError(null)}></button>
                </div>
            )}

            {/* Stats */}
            <div className="row g-3 mb-4">
                <div className="col-md-3">
                    <div className="card border-0 shadow-sm">
                        <div className="card-body text-center py-3">
                            <div className="fs-3 fw-bold text-primary">{orders.length}</div>
                            <div className="small text-secondary">Total Orders</div>
                        </div>
                    </div>
                </div>
                <div className="col-md-3">
                    <div className="card border-0 shadow-sm">
                        <div className="card-body text-center py-3">
                            <div className="fs-3 fw-bold text-warning">
                                {orders.filter(o => ['PENDING', 'PAYMENT_PENDING', 'CREATED'].includes(o.orderStatus)).length}
                            </div>
                            <div className="small text-secondary">Pending</div>
                        </div>
                    </div>
                </div>
                <div className="col-md-3">
                    <div className="card border-0 shadow-sm">
                        <div className="card-body text-center py-3">
                            <div className="fs-3 fw-bold text-info">
                                {orders.filter(o => ['PROCESSING', 'SHIPPED'].includes(o.orderStatus)).length}
                            </div>
                            <div className="small text-secondary">In Progress</div>
                        </div>
                    </div>
                </div>
                <div className="col-md-3">
                    <div className="card border-0 shadow-sm">
                        <div className="card-body text-center py-3">
                            <div className="fs-3 fw-bold text-success">
                                {orders.filter(o => o.orderStatus === 'DELIVERED').length}
                            </div>
                            <div className="small text-secondary">Delivered</div>
                        </div>
                    </div>
                </div>
            </div>

            <div className="row g-4">
                {/* Orders Table */}
                <div className={selectedOrder ? 'col-lg-7' : 'col-12'}>
                    <div className="card border-0 shadow-sm">
                        <div className="card-header bg-white py-3">
                            <div className="row g-3 align-items-center">
                                <div className="col-md-5">
                                    <div className="input-group input-group-sm">
                                        <span className="input-group-text bg-white">
                                            <i className="bi bi-search"></i>
                                        </span>
                                        <input
                                            type="text"
                                            className="form-control"
                                            placeholder="Search orders..."
                                            value={searchTerm}
                                            onChange={(e) => setSearchTerm(e.target.value)}
                                        />
                                    </div>
                                </div>
                                <div className="col-md-4">
                                    <select
                                        className="form-select form-select-sm"
                                        value={statusFilter}
                                        onChange={(e) => setStatusFilter(e.target.value)}
                                    >
                                        <option value="all">All Statuses</option>
                                        {statusOptions.map(status => (
                                            <option key={status} value={status}>{status}</option>
                                        ))}
                                    </select>
                                </div>
                                <div className="col-md-3 text-end">
                                    <span className="text-secondary small">{filteredOrders.length} orders</span>
                                </div>
                            </div>
                        </div>
                        <div className="card-body p-0">
                            {filteredOrders.length === 0 ? (
                                <div className="text-center py-5 text-secondary">
                                    <i className="bi bi-inbox fs-1 d-block mb-2"></i>
                                    <span>No orders found</span>
                                </div>
                            ) : (
                                <div className="table-responsive">
                                    <table className="table table-hover mb-0">
                                        <thead className="bg-light">
                                            <tr>
                                                <th className="border-0 ps-3" style={{ fontSize: '12px' }}>Order</th>
                                                <th className="border-0" style={{ fontSize: '12px' }}>Customer</th>
                                                <th className="border-0" style={{ fontSize: '12px' }}>Status</th>
                                                <th className="border-0" style={{ fontSize: '12px' }}>Total</th>
                                                <th className="border-0" style={{ fontSize: '12px' }}>Date</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {filteredOrders.map((order) => (
                                                <tr
                                                    key={order.orderId}
                                                    className={selectedOrder?.orderId === order.orderId ? 'table-active' : ''}
                                                    style={{ cursor: 'pointer' }}
                                                    onClick={() => handleSelectOrder(order)}
                                                >
                                                    <td className="ps-3">
                                                        <span className="fw-medium">#{order.orderId}</span>
                                                    </td>
                                                    <td>
                                                        <div className="small">{order.customerEmail || 'N/A'}</div>
                                                    </td>
                                                    <td>
                                                        <span
                                                            className="badge rounded-pill"
                                                            style={{
                                                                backgroundColor: getStatusColor(order.orderStatus),
                                                                fontSize: '10px'
                                                            }}
                                                        >
                                                            {order.orderStatus}
                                                        </span>
                                                    </td>
                                                    <td>
                                                        <span className="small">{formatCurrency(order.totalAmount)}</span>
                                                    </td>
                                                    <td>
                                                        <span className="small text-secondary">{formatDate(order.createdAt)}</span>
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

                {/* Order Detail Panel */}
                {selectedOrder && (
                    <div className="col-lg-5">
                        <div className="card border-0 shadow-sm sticky-top" style={{ top: '80px' }}>
                            <div className="card-header bg-white py-3 d-flex align-items-center justify-content-between">
                                <h6 className="fw-semibold mb-0">Order #{selectedOrder.orderId}</h6>
                                <button
                                    className="btn btn-sm btn-link text-secondary p-0"
                                    onClick={() => setSelectedOrder(null)}
                                >
                                    <i className="bi bi-x-lg"></i>
                                </button>
                            </div>
                            <div className="card-body">
                                {/* Current Status */}
                                <div className="text-center mb-4 py-3 bg-light rounded-3">
                                    <div className="small text-secondary mb-1">Current Status</div>
                                    <span
                                        className="badge rounded-pill fs-6 px-4 py-2"
                                        style={{ backgroundColor: getStatusColor(selectedOrder.orderStatus) }}
                                    >
                                        {selectedOrder.orderStatus}
                                    </span>
                                </div>

                                {/* Order Details */}
                                <div className="mb-4">
                                    <h6 className="fw-semibold small mb-3">Order Details</h6>
                                    <div className="row g-2 small">
                                        <div className="col-6">
                                            <div className="text-secondary">Customer</div>
                                            <div className="fw-medium">{selectedOrder.customerEmail || 'N/A'}</div>
                                        </div>
                                        <div className="col-6">
                                            <div className="text-secondary">Total</div>
                                            <div className="fw-medium">{formatCurrency(selectedOrder.totalAmount)}</div>
                                        </div>
                                        <div className="col-12 mt-2">
                                            <div className="text-secondary">Shipping Address</div>
                                            <div className="fw-medium">{selectedOrder.shippingAddress || 'N/A'}</div>
                                        </div>
                                        <div className="col-6">
                                            <div className="text-secondary">Created</div>
                                            <div className="fw-medium">{formatDate(selectedOrder.createdAt)}</div>
                                        </div>
                                        <div className="col-6">
                                            <div className="text-secondary">Payment</div>
                                            <div className="fw-medium">{selectedOrder.paymentMethod || 'N/A'}</div>
                                        </div>
                                    </div>
                                </div>

                                {/* Workflow Transitions */}
                                {orderWorkflow && (
                                    <div className="mb-4">
                                        <h6 className="fw-semibold small mb-3">
                                            <i className="bi bi-diagram-3 me-2 text-primary"></i>
                                            Workflow Actions
                                        </h6>
                                        {transitions.length === 0 ? (
                                            <div className="text-center py-3 bg-light rounded-3 text-secondary small">
                                                No transitions available
                                            </div>
                                        ) : (
                                            <div className="d-flex flex-wrap gap-2">
                                                {transitions.map((transition) => (
                                                    <button
                                                        key={transition.id}
                                                        className="btn btn-sm btn-outline-primary"
                                                        onClick={() => handleTransition(transition.toStateName)}
                                                        disabled={transitioning}
                                                    >
                                                        {transition.actionName}
                                                        <i className="bi bi-arrow-right ms-1"></i>
                                                    </button>
                                                ))}
                                            </div>
                                        )}

                                        {/* View workflow details link */}
                                        <div className="mt-3">
                                            <Link
                                                to={`/ops/workflows/${orderWorkflow.id}`}
                                                className="btn btn-sm btn-link p-0"
                                            >
                                                <i className="bi bi-clock-history me-1"></i>
                                                View Workflow Timeline
                                            </Link>
                                        </div>
                                    </div>
                                )}

                                {/* Legacy Status Update (fallback) */}
                                {!orderWorkflow && (
                                    <div className="mb-4">
                                        <div className="alert alert-info py-2 small mb-3">
                                            <i className="bi bi-info-circle me-1"></i>
                                            This order doesn't have a workflow. Use legacy status or create one.
                                        </div>

                                        <h6 className="fw-semibold small mb-3">Update Status (Legacy)</h6>
                                        <select
                                            className="form-select form-select-sm mb-3"
                                            value={selectedOrder.orderStatus}
                                            onChange={(e) => handleLegacyStatusUpdate(selectedOrder.orderId, e.target.value)}
                                        >
                                            <option value="PENDING">PENDING</option>
                                            <option value="CONFIRMED">CONFIRMED</option>
                                            <option value="PROCESSING">PROCESSING</option>
                                            <option value="SHIPPED">SHIPPED</option>
                                            <option value="DELIVERED">DELIVERED</option>
                                            <option value="CANCELLED">CANCELLED</option>
                                        </select>

                                        <div className="border-top pt-3">
                                            <button
                                                className="btn btn-outline-primary btn-sm w-100"
                                                onClick={() => handleCreateWorkflow(selectedOrder.orderId)}
                                                disabled={creatingWorkflow}
                                            >
                                                {creatingWorkflow ? (
                                                    <>
                                                        <span className="spinner-border spinner-border-sm me-1"></span>
                                                        Creating...
                                                    </>
                                                ) : (
                                                    <>
                                                        <i className="bi bi-plus-circle me-1"></i>
                                                        Create Workflow Instance
                                                    </>
                                                )}
                                            </button>
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

export default OpsOrdersPage;
