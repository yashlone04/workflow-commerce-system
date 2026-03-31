import React, { useState, useEffect, useRef } from "react";
import adminOrderService from "../../services/adminOrder.service";
import AuthService from "../../services/auth.service";
import { useNavigate } from "react-router-dom";
import { shouldFetch } from "../../utils/globalFetchTracker";

let isFetchingOrders = false;

const OrderDashboard = () => {
    const [orders, setOrders] = useState([]);
    const [filteredOrders, setFilteredOrders] = useState([]);
    const [statusFilter, setStatusFilter] = useState("all");
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState("");
    const [user, setUser] = useState(null);
    const navigate = useNavigate();
    const hasFetched = useRef(false);

    // Check if user is authenticated as admin
    useEffect(() => {
        console.log('OrderDashboard: User auth useEffect triggered', {
            timestamp: new Date().toISOString()
        });
        const currentUser = AuthService.getCurrentUser();
        console.log('OrderDashboard: AuthService.getCurrentUser() returned:', currentUser ? {
            id: currentUser.id,
            username: currentUser.username,
            hasToken: !!currentUser.token
        } : null);
        setUser(currentUser);
    }, []);

    useEffect(() => {
        console.log('OrderDashboard: useEffect triggered', {
            user: user ? { id: user.id } : null,
            hasFetched: hasFetched.current,
            timestamp: new Date().toISOString()
        });

        // Prevent any fetches after first successful fetch
        if (hasFetched.current) {
            console.log('OrderDashboard: Already fetched, skipping');
            return;
        }

        // Only proceed if user is ready
        if (!user?.id || !user?.token) {
            console.log('OrderDashboard: User not ready');
            return;
        }

        if (user.roles?.includes("ROLE_ADMIN")) {
            try {
                const tokenPayload = JSON.parse(atob(user.token.split('.')[1]));
                if (tokenPayload.exp < Date.now() / 1000) {
                    setMessage("Session expired. Please log in again.");
                    setTimeout(() => navigate("/login"), 1000);
                    return;
                }
                hasFetched.current = true;
                fetchOrders();
            } catch (error) {
                setMessage("Invalid session. Please log in again.");
                setTimeout(() => navigate("/login"), 1000);
            }
        }
    }, [user?.id]);

    useEffect(() => {
        if (statusFilter === "all") {
            setFilteredOrders(orders);
        } else {
            setFilteredOrders(orders.filter(order => order.orderStatus === statusFilter));
        }
    }, [statusFilter, orders]);

    // Consolidated early return after all hooks
    if (!user) {
        return (
            <div className="container py-5 text-center">
                <div className="admin-card p-5 d-inline-block shadow-sm">
                    <h2 className="text-danger fw-bold mb-3">Authentication Required</h2>
                    <p className="text-secondary mb-4">Please log in to access the admin dashboard.</p>
                    <button
                        className="btn-primary-tech px-4 py-2"
                        onClick={() => navigate("/login")}
                    >
                        Login to Continue
                    </button>
                </div>
            </div>
        );
    }

    if (!user.roles || !user.roles.includes("ROLE_ADMIN")) {
        return (
            <div className="container py-5 text-center">
                <div className="admin-card p-5 d-inline-block shadow-sm">
                    <h2 className="text-danger fw-bold mb-3">Access Denied</h2>
                    <p className="text-secondary mb-4">You do not have the required administrative permissions to access this console.</p>
                    <button
                        className="btn-primary-tech px-4 py-2"
                        onClick={() => navigate("/home")}
                    >
                        Return Home
                    </button>
                </div>
            </div>
        );
    }

    const fetchOrders = async () => {
        if (loading) {
            console.log('OrderDashboard: fetchOrders called while loading, skipping');
            return;
        }

        console.log('OrderDashboard: fetchOrders called');

        try {
            setLoading(true);
            const res = await adminOrderService.getAllOrders();
            setOrders(Array.isArray(res.data) ? res.data : []);
            console.log('OrderDashboard: fetchOrders completed successfully');
        } catch (err) {
            console.error("Failed to fetch orders", err);
            setMessage("Failed to load orders");
        } finally {
            setLoading(false);
        }
    };

    const handleStatusUpdate = async (orderId, newStatus) => {
        try {
            await adminOrderService.updateOrderStatus(orderId, newStatus);
            setMessage(`Order ${orderId} status updated to ${newStatus}`);
            fetchOrders(); // Refresh orders
        } catch (err) {
            setMessage(err.response?.data?.message || "Failed to update order status");
        }
    };

    const handleCancelOrder = async (orderId) => {
        if (!window.confirm("Are you sure you want to cancel this order?")) {
            return;
        }

        try {
            await adminOrderService.cancelAnyOrder(orderId);
            setMessage(`Order ${orderId} cancelled successfully`);
            fetchOrders(); // Refresh orders
        } catch (err) {
            setMessage(err.response?.data?.message || "Failed to cancel order");
        }
    };

    const getStatusBadge = (status) => {
        const statusClasses = {
            'Pending': 'bg-warning',
            'Shipped': 'bg-info',
            'Delivered': 'bg-success',
            'Cancelled': 'bg-danger'
        };
        return statusClasses[status] || 'bg-secondary';
    };

    const canCancel = (status) => {
        return status !== 'Shipped' && status !== 'Delivered';
    };

    if (loading) {
        return (
            <div className="container py-5">
                <div className="text-center">
                    <div className="spinner-border text-primary" role="status">
                        <span className="visually-hidden">Loading...</span>
                    </div>
                    <p className="mt-3">Loading orders...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="container py-5">
            <h2 className="mb-4">Order Management</h2>
            {message && <div className="alert alert-info">{message}</div>}

            <div className="admin-card p-4 shadow-sm mb-4">
                <div className="row mb-4">
                    <div className="col-md-4">
                        <label htmlFor="statusFilter" className="form-label">
                            Filter by Status
                        </label>
                        <select
                            id="statusFilter"
                            className="form-select"
                            value={statusFilter}
                            onChange={(e) => setStatusFilter(e.target.value)}
                        >
                            <option value="all">All Orders</option>
                            <option value="Pending">Pending</option>
                            <option value="Shipped">Shipped</option>
                            <option value="Delivered">Delivered</option>
                            <option value="Cancelled">Cancelled</option>
                        </select>
                    </div>
                    <div className="col-md-8">
                        <div className="text-muted small">
                            Showing {filteredOrders.length} of {orders.length} orders
                        </div>
                    </div>
                </div>
            </div>

            {filteredOrders.length === 0 ? (
                <div className="admin-card p-5 text-center shadow-sm">
                    <h4 className="text-secondary mb-3">No Orders Found</h4>
                    <p className="text-muted">
                        {statusFilter === "all"
                            ? "No orders have been placed yet."
                            : `No orders with status "${statusFilter}" found.`
                        }
                    </p>
                </div>
            ) : (
                <div className="admin-card p-4 shadow-sm">
                    <div className="table-responsive">
                        <table className="table">
                            <thead>
                                <tr>
                                    <th>Order ID</th>
                                    <th>Customer</th>
                                    <th>Total Amount</th>
                                    <th>Shipping Address</th>
                                    <th>Status</th>
                                    <th>Created Date</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {filteredOrders.map((order) => (
                                    <tr key={order.orderId}>
                                        <td>#{order.orderId}</td>
                                        <td>{order.user?.username || 'N/A'}</td>
                                        <td>${order.totalAmount ? order.totalAmount.toFixed(2) : '0.00'}</td>
                                        <td className="text-truncate" style={{ maxWidth: '200px' }} title={order.shippingAddress}>
                                            {order.shippingAddress}
                                        </td>
                                        <td>
                                            <span className={`badge ${getStatusBadge(order.orderStatus)}`}>
                                                {order.orderStatus}
                                            </span>
                                        </td>
                                        <td>{new Date(order.createdAt).toLocaleDateString()}</td>
                                        <td>
                                            <div className="btn-group" role="group">
                                                <select
                                                    className="form-select form-select-sm"
                                                    value={order.orderStatus}
                                                    onChange={(e) => handleStatusUpdate(order.orderId, e.target.value)}
                                                    title="Update Status"
                                                >
                                                    <option value="Pending">Pending</option>
                                                    <option value="Shipped">Shipped</option>
                                                    <option value="Delivered">Delivered</option>
                                                </select>

                                                {canCancel(order.orderStatus) && (
                                                    <button
                                                        className="btn btn-sm btn-danger ms-1"
                                                        onClick={() => handleCancelOrder(order.orderId)}
                                                        title="Cancel Order"
                                                    >
                                                        Cancel
                                                    </button>
                                                )}
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}
        </div>
    );
};

export default OrderDashboard;
