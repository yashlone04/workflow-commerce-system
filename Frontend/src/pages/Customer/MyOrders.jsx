import React, { useState, useEffect } from "react";
import orderService from "../../services/order.service";
import paymentService from "../../services/payment.service";
import shippingService from "../../services/shipping.service";
import AuthService from "../../services/auth.service";
import { useNavigate } from "react-router-dom";
import { validateAuth, forceLogout } from "../../utils/authUtils";

const MyOrders = () => {
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState("");
    const [user, setUser] = useState(null);
    const [hasFetched, setHasFetched] = useState(false);
    const [showPaymentModal, setShowPaymentModal] = useState(false);
    const [selectedOrder, setSelectedOrder] = useState(null);
    const [paymentMethod, setPaymentMethod] = useState("Credit Card");
    const [processingPayment, setProcessingPayment] = useState(false);

    // Shipping tracking state
    const [showShippingModal, setShowShippingModal] = useState(false);
    const [trackingInfo, setTrackingInfo] = useState(null);
    const navigate = useNavigate();

    // Check if user is authenticated
    useEffect(() => {
        if (hasFetched) return;

        const currentUser = AuthService.getCurrentUser();
        setUser(currentUser);

        if (!currentUser?.id || !currentUser?.token) {
            setLoading(false);
            setMessage("Please log in to view your orders");
            return;
        }

        // Check if token is expired
        try {
            const tokenPayload = JSON.parse(atob(currentUser.token.split('.')[1]));
            if (tokenPayload.exp < Date.now() / 1000) {
                setMessage("Your session has expired. Please log in again.");
                setTimeout(() => {
                    localStorage.removeItem('user');
                    navigate("/login");
                }, 1000);
                return;
            }
            setHasFetched(true);
            fetchOrders();
        } catch (error) {
            setMessage("Invalid session. Please log in again.");
            setTimeout(() => navigate("/login"), 1000);
        }
    }, [hasFetched]);

    const fetchOrders = async () => {
        if (loading) return; // Prevent multiple simultaneous requests

        try {
            setLoading(true);
            const res = await orderService.getMyOrders();
            setOrders(Array.isArray(res.data) ? res.data : []);
        } catch (err) {
            console.error("Failed to fetch orders", err);
            if (err.response?.status === 403) {
                setMessage("Your session has expired. Please log in again.");
                setTimeout(() => navigate("/login"), 2000);
            } else {
                setMessage("Failed to load orders");
            }
        } finally {
            setLoading(false);
        }
    };

    const handleCancelOrder = async (orderId) => {
        if (!window.confirm("Are you sure you want to cancel this order?")) {
            return;
        }

        try {
            await orderService.cancelOrder(orderId);
            setMessage("Order cancelled successfully");
            fetchOrders(); // Refresh orders
        } catch (err) {
            setMessage(err.response?.data?.message || "Failed to cancel order");
        }
    };

    const getStatusBadge = (status) => {
        const statusClasses = {
            'Pending': 'bg-warning',
            'Paid': 'bg-success',
            'Shipped': 'bg-info',
            'Delivered': 'bg-success',
            'Cancelled': 'bg-danger'
        };
        return statusClasses[status] || 'bg-secondary';
    };

    const handlePayClick = (order) => {
        setSelectedOrder(order);
        setPaymentMethod("Credit Card");
        setShowPaymentModal(true);
    };

    const handlePaymentSubmit = async () => {
        if (!selectedOrder) return;

        try {
            setProcessingPayment(true);
            const response = await paymentService.processPayment(selectedOrder.orderId, paymentMethod);
            setMessage(response.data.message);
            setShowPaymentModal(false);
            setSelectedOrder(null);
            fetchOrders();
        } catch (err) {
            setMessage(err.response?.data?.message || "Payment processing failed.");
        } finally {
            setProcessingPayment(false);
        }
    };

    const handleTrackShipping = async (orderId) => {
        try {
            const res = await shippingService.getMyShipping(orderId);
            setTrackingInfo(res.data);
            setShowShippingModal(true);
        } catch (err) {
            setMessage("Shipping details not available yet.");
        }
    };

    // Check if user is authenticated before showing content
    if (!user) {
        return (
            <div className="container py-5">
                <div className="admin-card p-5 text-center shadow-sm">
                    <h2 className="text-danger fw-bold mb-3">Authentication Required</h2>
                    <p className="text-secondary mb-4">Please log in to view your orders.</p>
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

    if (loading) {
        return (
            <div className="container py-5">
                <div className="text-center">
                    <div className="spinner-border text-primary" role="status">
                        <span className="visually-hidden">Loading...</span>
                    </div>
                    <p className="mt-3">Loading your orders...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="container py-5">
            <h2 className="mb-4">My Orders</h2>
            {message && <div className="alert alert-info">{message}</div>}

            {orders.length === 0 ? (
                <div className="admin-card p-5 text-center shadow-sm">
                    <h4 className="text-secondary mb-3">No Orders Found</h4>
                    <p className="text-muted">You haven't placed any orders yet.</p>
                </div>
            ) : (
                <div className="admin-card p-4 shadow-sm">
                    <div className="table-responsive">
                        <table className="table">
                            <thead>
                                <tr>
                                    <th>Order ID</th>
                                    <th>Total Amount</th>
                                    <th>Status</th>
                                    <th>Created Date</th>
                                    <th>Action</th>
                                </tr>
                            </thead>
                            <tbody>
                                {orders.map((order) => (
                                    <tr key={order.orderId}>
                                        <td>#{order.orderId}</td>
                                        <td>${order.totalAmount ? order.totalAmount.toFixed(2) : '0.00'}</td>
                                        <td>
                                            <span className={`badge ${getStatusBadge(order.orderStatus)}`}>
                                                {order.orderStatus}
                                            </span>
                                        </td>
                                        <td>{new Date(order.createdAt).toLocaleDateString()}</td>
                                        <td>
                                            {order.orderStatus === 'Pending' && (
                                                <>
                                                    <button
                                                        className="btn btn-sm btn-success me-2"
                                                        onClick={() => handlePayClick(order)}
                                                    >
                                                        Pay Now
                                                    </button>
                                                    <button
                                                        className="btn btn-sm btn-danger"
                                                        onClick={() => handleCancelOrder(order.orderId)}
                                                    >
                                                        Cancel
                                                    </button>
                                                </>
                                            )}
                                            {['Shipped', 'In Transit', 'Delivered'].includes(order.orderStatus) && (
                                                <button
                                                    className="btn btn-sm btn-outline-info"
                                                    onClick={() => handleTrackShipping(order.orderId)}
                                                >
                                                    Track Shipping
                                                </button>
                                            )}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            {/* Payment Modal */}
            {showPaymentModal && selectedOrder && (
                <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
                    <div className="modal-dialog modal-dialog-centered">
                        <div className="modal-content">
                            <div className="modal-header">
                                <h5 className="modal-title">Process Payment</h5>
                                <button type="button" className="btn-close" onClick={() => setShowPaymentModal(false)}></button>
                            </div>
                            <div className="modal-body">
                                <p><strong>Order ID:</strong> #{selectedOrder.orderId}</p>
                                <p><strong>Amount:</strong> ${selectedOrder.totalAmount ? selectedOrder.totalAmount.toFixed(2) : '0.00'}</p>
                                <div className="mb-3">
                                    <label className="form-label">Payment Method</label>
                                    <select
                                        className="form-select"
                                        value={paymentMethod}
                                        onChange={(e) => setPaymentMethod(e.target.value)}
                                    >
                                        <option value="Credit Card">Credit Card</option>
                                        <option value="PayPal">PayPal</option>
                                        <option value="Bank Transfer">Bank Transfer</option>
                                    </select>
                                </div>
                                <p className="text-muted small">Note: Payment simulation has 90% success rate.</p>
                            </div>
                            <div className="modal-footer">
                                <button
                                    type="button"
                                    className="btn btn-secondary"
                                    onClick={() => setShowPaymentModal(false)}
                                    disabled={processingPayment}
                                >
                                    Cancel
                                </button>
                                <button
                                    type="button"
                                    className="btn btn-success"
                                    onClick={handlePaymentSubmit}
                                    disabled={processingPayment}
                                >
                                    {processingPayment ? (
                                        <>
                                            <span className="spinner-border spinner-border-sm me-2"></span>
                                            Processing...
                                        </>
                                    ) : (
                                        'Confirm Payment'
                                    )}
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* Shipping Tracking Modal */}
            {showShippingModal && trackingInfo && (
                <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
                    <div className="modal-dialog modal-dialog-centered">
                        <div className="modal-content">
                            <div className="modal-header">
                                <h5 className="modal-title">Track Shipment</h5>
                                <button type="button" className="btn-close" onClick={() => setShowShippingModal(false)}></button>
                            </div>
                            <div className="modal-body">
                                <div className="p-3 bg-light rounded shadow-sm border mb-3">
                                    <h6 className="fw-bold mb-3 border-bottom pb-2">Tracking Details</h6>
                                    <div className="row mb-2">
                                        <div className="col-5 text-muted small">Status:</div>
                                        <div className="col-7 fw-bold text-primary">{trackingInfo.shippingStatus}</div>
                                    </div>
                                    <div className="row mb-2">
                                        <div className="col-5 text-muted small">Courier:</div>
                                        <div className="col-7">{trackingInfo.courierService}</div>
                                    </div>
                                    <div className="row mb-2">
                                        <div className="col-5 text-muted small">Tracking Ref:</div>
                                        <div className="col-7">{trackingInfo.trackingNumber}</div>
                                    </div>
                                </div>
                            </div>
                            <div className="modal-footer">
                                <button type="button" className="btn btn-secondary" onClick={() => setShowShippingModal(false)}>Close</button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default MyOrders;
