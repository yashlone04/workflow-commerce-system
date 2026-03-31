import React, { useState, useEffect } from "react";
import shippingService from "../../services/shipping.service";
import adminOrderService from "../../services/adminOrder.service";

// Shipping Progress Timeline Component with tick/checkmarks
const ShippingProgressTimeline = ({ currentStatus }) => {
    const stages = [
        { key: "Shipped", label: "Processing/Shipped", icon: "📦" },
        { key: "In Transit", label: "In Transit", icon: "🚚" },
        { key: "Delivered", label: "Delivered", icon: "✅" }
    ];

    const getStageIndex = (status) => {
        const idx = stages.findIndex(s => s.key === status);
        return idx >= 0 ? idx : 0;
    };

    const currentIndex = getStageIndex(currentStatus);

    return (
        <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0',
            padding: '8px 0'
        }}>
            {stages.map((stage, index) => {
                const isCompleted = index < currentIndex;
                const isCurrent = index === currentIndex;
                const isPending = index > currentIndex;

                return (
                    <React.Fragment key={stage.key}>
                        {/* Stage Circle */}
                        <div style={{
                            display: 'flex',
                            flexDirection: 'column',
                            alignItems: 'center',
                            position: 'relative'
                        }}>
                            <div style={{
                                width: '36px',
                                height: '36px',
                                borderRadius: '50%',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                fontSize: '16px',
                                fontWeight: 'bold',
                                background: isCompleted
                                    ? 'linear-gradient(135deg, #28a745, #20c997)'
                                    : isCurrent
                                        ? 'linear-gradient(135deg, #007bff, #6f42c1)'
                                        : '#e9ecef',
                                color: isCompleted || isCurrent ? 'white' : '#6c757d',
                                border: isCurrent ? '3px solid #007bff' : 'none',
                                boxShadow: isCompleted
                                    ? '0 2px 8px rgba(40, 167, 69, 0.4)'
                                    : isCurrent
                                        ? '0 2px 12px rgba(0, 123, 255, 0.5)'
                                        : 'none',
                                transition: 'all 0.3s ease'
                            }}>
                                {isCompleted ? '✓' : stage.icon}
                            </div>
                            <span style={{
                                fontSize: '11px',
                                marginTop: '4px',
                                color: isCompleted ? '#28a745' : isCurrent ? '#007bff' : '#6c757d',
                                fontWeight: isCompleted || isCurrent ? '600' : '400',
                                textAlign: 'center',
                                maxWidth: '70px'
                            }}>
                                {stage.label}
                            </span>
                        </div>

                        {/* Connector Line */}
                        {index < stages.length - 1 && (
                            <div style={{
                                flex: '1',
                                height: '4px',
                                minWidth: '30px',
                                maxWidth: '60px',
                                background: index < currentIndex
                                    ? 'linear-gradient(90deg, #28a745, #20c997)'
                                    : '#dee2e6',
                                borderRadius: '2px',
                                margin: '0 4px',
                                marginBottom: '20px',
                                transition: 'background 0.3s ease'
                            }} />
                        )}
                    </React.Fragment>
                );
            })}
        </div>
    );
};

const ShippingDashboard = () => {
    const [shippings, setShippings] = useState([]);
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState("");
    const [filterStatus, setFilterStatus] = useState("All");

    const [showCreateModal, setShowCreateModal] = useState(false);
    const [selectedOrderId, setSelectedOrderId] = useState("");
    const [newShipping, setNewShipping] = useState({
        courierService: "",
        trackingNumber: "",
        shippingMethod: "Standard"
    });
    const [calculatedCost, setCalculatedCost] = useState(null);

    const [showUpdateModal, setShowUpdateModal] = useState(false);
    const [selectedShipping, setSelectedShipping] = useState(null);
    const [updateStatus, setUpdateStatus] = useState("");

    useEffect(() => {
        fetchShippings();
        fetchOrders(); // We need orders to populate the Create Shipping dropdown
    }, []);

    const fetchShippings = async () => {
        setLoading(true);
        try {
            const res = await shippingService.getAllShippings();
            setShippings(Array.isArray(res.data) ? res.data : []);
            setMessage("");
        } catch (err) {
            setMessage(err.response?.data?.error || "Failed to load shipping data.");
        } finally {
            setLoading(false);
        }
    };

    const fetchOrders = async () => {
        // Fetch orders and filter those with "Paid" status so they can be shipped
        try {
            const res = await adminOrderService.getAllOrders();
            if (Array.isArray(res.data)) {
                // Only Paid orders can be shipped
                setOrders(res.data.filter(order => order.orderStatus === "Paid"));
            }
        } catch (err) {
            console.error("Failed to load orders", err);
        }
    };

    const handleCalculateCost = async () => {
        if (!selectedOrderId) return;
        const o = orders.find(o => String(o.orderId) === String(selectedOrderId));
        if (!o) return;

        try {
            const res = await shippingService.calculateShippingCost(newShipping.shippingMethod, o.shippingAddress);
            setCalculatedCost(res.data.cost);
        } catch (err) {
            setMessage("Failed to calculate cost");
        }
    };

    const handleCreateShipping = async () => {
        if (!selectedOrderId || !newShipping.courierService || !newShipping.trackingNumber) {
            setMessage("Please fill all fields to create shipping.");
            return;
        }

        try {
            await shippingService.createShipping(selectedOrderId, newShipping);
            setMessage("Shipping created successfully");
            setShowCreateModal(false);
            setNewShipping({ courierService: "", trackingNumber: "", shippingMethod: "Standard" });
            setCalculatedCost(null);
            fetchShippings();
            fetchOrders();
        } catch (err) {
            setMessage(err.response?.data?.error || "Failed to create shipping.");
        }
    };

    const handleUpdateStatus = async () => {
        try {
            await shippingService.updateShippingStatus(selectedShipping.id, updateStatus);
            setMessage("Shipping status updated successfully");
            setShowUpdateModal(false);
            fetchShippings();
        } catch (err) {
            setMessage(err.response?.data?.error || "Failed to update shipping status.");
        }
    };

    const filteredShippings = filterStatus === "All"
        ? shippings
        : shippings.filter(s => s.shippingStatus === filterStatus);

    return (
        <div className="container py-5">
            <div className="d-flex justify-content-between align-items-center mb-4">
                <h2>Shipping Dashboard</h2>
                <button className="btn btn-primary" onClick={() => setShowCreateModal(true)}>
                    Create Shipping
                </button>
            </div>

            {message && <div className="alert alert-info">{message}</div>}

            <div className="admin-card p-4 shadow-sm mb-4">
                <div className="d-flex mb-3 align-items-center">
                    <label className="me-2 fw-bold">Filter By Status:</label>
                    <select
                        className="form-select w-auto"
                        value={filterStatus}
                        onChange={(e) => setFilterStatus(e.target.value)}
                    >
                        <option value="All">All</option>
                        <option value="Shipped">Shipped</option>
                        <option value="In Transit">In Transit</option>
                        <option value="Delivered">Delivered</option>
                    </select>
                </div>

                {loading ? (
                    <div>Loading shipping data...</div>
                ) : (
                    <div className="table-responsive">
                        <table className="table table-hover">
                            <thead>
                                <tr>
                                    <th>Shipping ID</th>
                                    <th>Order ID</th>
                                    <th>Courier</th>
                                    <th>Tracking Number</th>
                                    <th>Cost</th>
                                    <th style={{ minWidth: '280px' }}>Progress</th>
                                    <th>Action</th>
                                </tr>
                            </thead>
                            <tbody>
                                {filteredShippings.map((shipping) => (
                                    <tr key={shipping.id}>
                                        <td>#{shipping.id}</td>
                                        <td>#{shipping.orderId}</td>
                                        <td>{shipping.courierService}</td>
                                        <td>{shipping.trackingNumber}</td>
                                        <td>${shipping.shippingCost}</td>
                                        <td>
                                            <ShippingProgressTimeline currentStatus={shipping.shippingStatus} />
                                        </td>
                                        <td>
                                            {shipping.shippingStatus !== 'Delivered' && (
                                                <button
                                                    className="btn btn-sm btn-outline-secondary"
                                                    onClick={() => {
                                                        setSelectedShipping(shipping);
                                                        setUpdateStatus(shipping.shippingStatus === "Shipped" ? "In Transit" : "Delivered");
                                                        setShowUpdateModal(true);
                                                    }}
                                                >
                                                    Update Status
                                                </button>
                                            )}
                                        </td>
                                    </tr>
                                ))}
                                {filteredShippings.length === 0 && (
                                    <tr>
                                        <td colSpan="7" className="text-center">No shipping records found.</td>
                                    </tr>
                                )}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

            {/* Create Shipping Modal */}
            {showCreateModal && (
                <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
                    <div className="modal-dialog">
                        <div className="modal-content">
                            <div className="modal-header">
                                <h5 className="modal-title">Create Shipping</h5>
                                <button type="button" className="btn-close" onClick={() => setShowCreateModal(false)}></button>
                            </div>
                            <div className="modal-body">
                                <div className="mb-3">
                                    <label className="form-label">Select Order (Paid only)</label>
                                    <select
                                        className="form-select"
                                        value={selectedOrderId}
                                        onChange={(e) => {
                                            setSelectedOrderId(e.target.value);
                                            setCalculatedCost(null);
                                        }}
                                    >
                                        <option value="">-- Select Order --</option>
                                        {orders.map(o => (
                                            <option key={o.orderId} value={o.orderId}>
                                                Order #{o.orderId} - {o.shippingAddress}
                                            </option>
                                        ))}
                                    </select>
                                </div>
                                <div className="mb-3">
                                    <label className="form-label">Courier Service</label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        value={newShipping.courierService}
                                        onChange={(e) => setNewShipping({ ...newShipping, courierService: e.target.value })}
                                    />
                                </div>
                                <div className="mb-3">
                                    <label className="form-label">Tracking Number</label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        value={newShipping.trackingNumber}
                                        onChange={(e) => setNewShipping({ ...newShipping, trackingNumber: e.target.value })}
                                    />
                                </div>
                                <div className="mb-3">
                                    <label className="form-label">Shipping Method</label>
                                    <select
                                        className="form-select"
                                        value={newShipping.shippingMethod}
                                        onChange={(e) => {
                                            setNewShipping({ ...newShipping, shippingMethod: e.target.value });
                                            setCalculatedCost(null);
                                        }}
                                    >
                                        <option value="Standard">Standard</option>
                                        <option value="Express">Express</option>
                                    </select>
                                </div>

                                <div className="mb-3">
                                    <button className="btn btn-outline-info btn-sm" onClick={handleCalculateCost} disabled={!selectedOrderId}>
                                        Calculate Cost
                                    </button>
                                    {calculatedCost !== null && (
                                        <span className="ms-3 fw-bold">Cost: ${calculatedCost}</span>
                                    )}
                                </div>
                            </div>
                            <div className="modal-footer">
                                <button type="button" className="btn btn-secondary" onClick={() => setShowCreateModal(false)}>Close</button>
                                <button type="button" className="btn btn-primary" onClick={handleCreateShipping}>Confirm Shipping</button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* Update Status Modal */}
            {showUpdateModal && selectedShipping && (
                <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
                    <div className="modal-dialog modal-lg">
                        <div className="modal-content">
                            <div className="modal-header" style={{ background: 'linear-gradient(135deg, #667eea, #764ba2)', color: 'white' }}>
                                <h5 className="modal-title">Update Shipping Status</h5>
                                <button type="button" className="btn-close btn-close-white" onClick={() => setShowUpdateModal(false)}></button>
                            </div>
                            <div className="modal-body">
                                <div className="row mb-3">
                                    <div className="col-6">
                                        <p><strong>Order ID:</strong> #{selectedShipping.orderId}</p>
                                    </div>
                                    <div className="col-6">
                                        <p><strong>Tracking:</strong> {selectedShipping.trackingNumber}</p>
                                    </div>
                                </div>

                                {/* Current Progress Timeline */}
                                <div className="mb-4 p-3" style={{ background: '#f8f9fa', borderRadius: '8px' }}>
                                    <p className="mb-2 fw-bold text-muted small">Current Progress:</p>
                                    <ShippingProgressTimeline currentStatus={selectedShipping.shippingStatus} />
                                </div>

                                {/* Status Selection - Only allow sequential transitions */}
                                <div className="mb-3">
                                    <label className="form-label fw-bold">Move to Next Step:</label>
                                    <div className="d-flex gap-2">
                                        {selectedShipping.shippingStatus === "Shipped" && (
                                            <button
                                                className={`btn flex-fill ${updateStatus === "In Transit" ? 'btn-primary' : 'btn-outline-primary'}`}
                                                onClick={() => setUpdateStatus("In Transit")}
                                            >
                                                🚚 In Transit
                                            </button>
                                        )}
                                        {selectedShipping.shippingStatus === "In Transit" && (
                                            <button
                                                className={`btn flex-fill ${updateStatus === "Delivered" ? 'btn-success' : 'btn-outline-success'}`}
                                                onClick={() => setUpdateStatus("Delivered")}
                                            >
                                                ✅ Delivered
                                            </button>
                                        )}
                                    </div>
                                    <small className="text-muted mt-2 d-block">
                                        <strong>Note:</strong> Status must progress sequentially: Shipped → In Transit → Delivered
                                    </small>
                                </div>

                                {/* Preview of new progress */}
                                {updateStatus && (
                                    <div className="p-3" style={{ background: '#e8f5e9', borderRadius: '8px' }}>
                                        <p className="mb-2 fw-bold text-success small">After Update:</p>
                                        <ShippingProgressTimeline currentStatus={updateStatus} />
                                    </div>
                                )}
                            </div>
                            <div className="modal-footer">
                                <button type="button" className="btn btn-secondary" onClick={() => setShowUpdateModal(false)}>Cancel</button>
                                <button type="button" className="btn btn-success" onClick={handleUpdateStatus}>
                                    Confirm Update
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default ShippingDashboard;
