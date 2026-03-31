import { useState, useEffect } from "react";
import paymentService from "../../services/payment.service";
import authService from "../../services/auth.service";
import { useNavigate } from "react-router-dom";

const PaymentDashboard = () => {
    const [payments, setPayments] = useState([]);
    const [filteredPayments, setFilteredPayments] = useState([]);
    const [statusFilter, setStatusFilter] = useState("all");
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [successMessage, setSuccessMessage] = useState("");
    const [showRefundModal, setShowRefundModal] = useState(false);
    const [selectedPayment, setSelectedPayment] = useState(null);
    const navigate = useNavigate();

    const currentUser = authService.getCurrentUser();

    useEffect(() => {
        if (!currentUser) {
            navigate("/login");
            return;
        }

        const isAdmin = currentUser.roles?.includes("ROLE_ADMIN");
        if (!isAdmin) {
            navigate("/");
            return;
        }

        fetchPayments();
    }, []);

    useEffect(() => {
        filterPayments();
    }, [payments, statusFilter]);

    const fetchPayments = async () => {
        try {
            setLoading(true);
            const response = await paymentService.getAllPayments();
            setPayments(response.data);
            setError(null);
        } catch (err) {
            if (err.response?.status === 401 || err.response?.status === 403) {
                authService.logout();
                navigate("/login");
            } else {
                setError("Failed to fetch payments. Please try again.");
            }
        } finally {
            setLoading(false);
        }
    };

    const filterPayments = () => {
        let filtered = [...payments];
        if (statusFilter !== "all") {
            filtered = filtered.filter(p => p.paymentStatus === statusFilter);
        }
        setFilteredPayments(filtered);
    };

    const handleRefundClick = (payment) => {
        setSelectedPayment(payment);
        setShowRefundModal(true);
    };

    const handleRefundConfirm = async () => {
        try {
            const response = await paymentService.refundPayment(selectedPayment.paymentId);
            setSuccessMessage(response.data.message);
            fetchPayments();
            setShowRefundModal(false);
            setSelectedPayment(null);
            setTimeout(() => setSuccessMessage(""), 3000);
        } catch (err) {
            setError(err.response?.data?.message || "Failed to process refund.");
            setTimeout(() => setError(null), 3000);
        }
    };

    const formatCurrency = (amount) => {
        return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'USD'
        }).format(amount);
    };

    const getStatusBadge = (status) => {
        const variants = {
            Paid: "bg-success",
            Failed: "bg-danger",
            Refunded: "bg-warning text-dark"
        };
        return <span className={`badge ${variants[status] || "bg-secondary"}`}>{status}</span>;
    };

    if (loading) {
        return (
            <div className="container mt-4">
                <div className="d-flex justify-content-center">
                    <div className="spinner-border" role="status">
                        <span className="visually-hidden">Loading...</span>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="container mt-4">
            <h2>Payment Management</h2>

            {successMessage && (
                <div className="alert alert-success alert-dismissible fade show" role="alert">
                    {successMessage}
                    <button type="button" className="btn-close" onClick={() => setSuccessMessage("")}></button>
                </div>
            )}

            {error && (
                <div className="alert alert-danger alert-dismissible fade show" role="alert">
                    {error}
                    <button type="button" className="btn-close" onClick={() => setError(null)}></button>
                </div>
            )}

            {/* Filters */}
            <div className="card mb-4">
                <div className="card-body">
                    <div className="row">
                        <div className="col-md-3">
                            <label className="form-label">Filter by Status</label>
                            <select
                                className="form-select"
                                value={statusFilter}
                                onChange={(e) => setStatusFilter(e.target.value)}
                            >
                                <option value="all">All Statuses</option>
                                <option value="Paid">Paid</option>
                                <option value="Failed">Failed</option>
                                <option value="Refunded">Refunded</option>
                            </select>
                        </div>
                    </div>
                </div>
            </div>

            {/* Payments Table */}
            <div className="card">
                <div className="card-header">
                    <h5 className="mb-0">All Payments</h5>
                </div>
                <div className="card-body">
                    <div className="table-responsive">
                        <table className="table table-striped table-hover">
                            <thead>
                                <tr>
                                    <th>Payment ID</th>
                                    <th>Order ID</th>
                                    <th>Customer</th>
                                    <th>Amount</th>
                                    <th>Method</th>
                                    <th>Status</th>
                                    <th>Date</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {filteredPayments.length === 0 ? (
                                    <tr>
                                        <td colSpan="8" className="text-center">No payments found.</td>
                                    </tr>
                                ) : (
                                    filteredPayments.map((payment) => (
                                        <tr key={payment.paymentId}>
                                            <td>{payment.paymentId}</td>
                                            <td>{payment.orderId}</td>
                                            <td>{payment.customerName}</td>
                                            <td>{formatCurrency(payment.amount)}</td>
                                            <td>{payment.paymentMethod}</td>
                                            <td>{getStatusBadge(payment.paymentStatus)}</td>
                                            <td>{new Date(payment.createdAt).toLocaleString()}</td>
                                            <td>
                                                {payment.paymentStatus === "Paid" && (
                                                    <button
                                                        className="btn btn-warning btn-sm"
                                                        onClick={() => handleRefundClick(payment)}
                                                    >
                                                        Refund
                                                    </button>
                                                )}
                                            </td>
                                        </tr>
                                    ))
                                )}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

            {/* Refund Confirmation Modal */}
            {showRefundModal && selectedPayment && (
                <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
                    <div className="modal-dialog modal-dialog-centered">
                        <div className="modal-content">
                            <div className="modal-header">
                                <h5 className="modal-title">Confirm Refund</h5>
                                <button type="button" className="btn-close" onClick={() => setShowRefundModal(false)}></button>
                            </div>
                            <div className="modal-body">
                                <p>Are you sure you want to refund this payment?</p>
                                <p><strong>Payment ID:</strong> {selectedPayment.paymentId}</p>
                                <p><strong>Order ID:</strong> {selectedPayment.orderId}</p>
                                <p><strong>Customer:</strong> {selectedPayment.customerName}</p>
                                <p><strong>Amount:</strong> {formatCurrency(selectedPayment.amount)}</p>
                                <p className="text-warning">This will update the order status to "Cancelled".</p>
                            </div>
                            <div className="modal-footer">
                                <button type="button" className="btn btn-secondary" onClick={() => setShowRefundModal(false)}>
                                    Cancel
                                </button>
                                <button type="button" className="btn btn-warning" onClick={handleRefundConfirm}>
                                    Confirm Refund
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default PaymentDashboard;
