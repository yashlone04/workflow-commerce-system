import React, { useState, useEffect } from "react";
import couponService from "../../services/coupon.service";

const CouponDashboard = () => {
    const [coupons, setCoupons] = useState([]);
    const [loading, setLoading] = useState(true);
    const [message, setMessage] = useState("");
    const [showModal, setShowModal] = useState(false);
    const [editMode, setEditMode] = useState(false);
    const [currentCoupon, setCurrentCoupon] = useState({
        couponCode: "",
        discountType: "Percentage",
        discountValue: "",
        minOrderAmount: "",
        validFrom: "",
        validTo: "",
        usageLimit: ""
    });

    useEffect(() => {
        fetchCoupons();
    }, []);

    const fetchCoupons = async () => {
        try {
            setLoading(true);
            const response = await couponService.getAllCoupons();
            setCoupons(response.data);
        } catch (error) {
            setMessage("Failed to load coupons");
        } finally {
            setLoading(false);
        }
    };

    const handleDeactivate = async (id) => {
        if (!window.confirm("Are you sure you want to deactivate this coupon?")) return;
        try {
            await couponService.deactivateCoupon(id);
            setMessage("Coupon deactivated successfully");
            fetchCoupons();
        } catch (error) {
            setMessage(error.response?.data?.error || "Error deactivating coupon");
        }
    };

    const handleEdit = (coupon) => {
        setEditMode(true);
        setCurrentCoupon({
            id: coupon.id,
            couponCode: coupon.couponCode,
            discountType: coupon.discountType,
            discountValue: coupon.discountValue,
            minOrderAmount: coupon.minOrderAmount || "",
            validFrom: coupon.validFrom.split('.')[0],
            validTo: coupon.validTo.split('.')[0],
            usageLimit: coupon.usageLimit
        });
        setShowModal(true);
    };

    const handleCreate = () => {
        setEditMode(false);
        setCurrentCoupon({
            couponCode: "",
            discountType: "Percentage",
            discountValue: "",
            minOrderAmount: "",
            validFrom: "",
            validTo: "",
            usageLimit: ""
        });
        setShowModal(true);
    };

    const handleSave = async (e) => {
        e.preventDefault();
        try {
            if (editMode) {
                await couponService.updateCoupon(currentCoupon.id, currentCoupon);
                setMessage("Coupon updated successfully");
            } else {
                await couponService.createCoupon(currentCoupon);
                setMessage("Coupon created successfully");
            }
            setShowModal(false);
            fetchCoupons();
        } catch (error) {
            alert(error.response?.data?.error || "Error saving coupon");
        }
    };

    return (
        <div className="container py-5">
            <div className="d-flex justify-content-between align-items-center mb-4">
                <h2 className="text-primary fw-bold">Coupon Management</h2>
                <button className="btn-primary-tech px-4 py-2" onClick={handleCreate}>
                    Create Coupon
                </button>
            </div>

            {message && <div className="alert alert-info">{message}</div>}

            <div className="card shadow-sm border-0">
                <div className="card-body p-0">
                    <div className="table-responsive">
                        <table className="table table-hover align-middle mb-0">
                            <thead className="table-light">
                                <tr>
                                    <th>Code</th>
                                    <th>Type</th>
                                    <th>Value</th>
                                    <th>Min Order</th>
                                    <th>Valid To</th>
                                    <th>Usage</th>
                                    <th>Status</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {coupons.map((coupon) => (
                                    <tr key={coupon.id}>
                                        <td className="fw-bold">{coupon.couponCode}</td>
                                        <td>{coupon.discountType}</td>
                                        <td>
                                            {coupon.discountType === 'Percentage' ? `${coupon.discountValue}%` : `$${coupon.discountValue}`}
                                        </td>
                                        <td>{coupon.minOrderAmount ? `$${coupon.minOrderAmount}` : 'None'}</td>
                                        <td>{new Date(coupon.validTo).toLocaleDateString()}</td>
                                        <td>{coupon.usageCount} / {coupon.usageLimit}</td>
                                        <td>
                                            <span className={`badge ${coupon.status ? 'bg-success' : 'bg-danger'}`}>
                                                {coupon.status ? 'Active' : 'Inactive'}
                                            </span>
                                        </td>
                                        <td>
                                            <button className="btn btn-sm btn-outline-primary me-2" onClick={() => handleEdit(coupon)}>
                                                Edit
                                            </button>
                                            {coupon.status && (
                                                <button className="btn btn-sm btn-outline-danger" onClick={() => handleDeactivate(coupon.id)}>
                                                    Deactivate
                                                </button>
                                            )}
                                        </td>
                                    </tr>
                                ))}
                                {coupons.length === 0 && !loading && (
                                    <tr>
                                        <td colSpan="8" className="text-center py-4 text-muted">No coupons found.</td>
                                    </tr>
                                )}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

            {/* Modal */}
            {showModal && (
                <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
                    <div className="modal-dialog">
                        <div className="modal-content">
                            <form onSubmit={handleSave}>
                                <div className="modal-header bg-primary text-white">
                                    <h5 className="modal-title">{editMode ? 'Edit Coupon' : 'Create Coupon'}</h5>
                                    <button type="button" className="btn-close btn-close-white" onClick={() => setShowModal(false)}></button>
                                </div>
                                <div className="modal-body">
                                    <div className="mb-3">
                                        <label className="form-label">Coupon Code</label>
                                        <input type="text" className="form-control" required value={currentCoupon.couponCode} onChange={e => setCurrentCoupon({ ...currentCoupon, couponCode: e.target.value.toUpperCase() })} />
                                    </div>
                                    <div className="row mb-3">
                                        <div className="col-md-6">
                                            <label className="form-label">Discount Type</label>
                                            <select className="form-select" value={currentCoupon.discountType} onChange={e => setCurrentCoupon({ ...currentCoupon, discountType: e.target.value })}>
                                                <option value="Percentage">Percentage</option>
                                                <option value="Fixed">Fixed Amount</option>
                                            </select>
                                        </div>
                                        <div className="col-md-6">
                                            <label className="form-label">Discount Value</label>
                                            <input type="number" step="0.01" className="form-control" required value={currentCoupon.discountValue} onChange={e => setCurrentCoupon({ ...currentCoupon, discountValue: e.target.value })} />
                                        </div>
                                    </div>
                                    <div className="row mb-3">
                                        <div className="col-md-6">
                                            <label className="form-label">Min Order Amount (Optional)</label>
                                            <input type="number" step="0.01" className="form-control" value={currentCoupon.minOrderAmount} onChange={e => setCurrentCoupon({ ...currentCoupon, minOrderAmount: e.target.value })} />
                                        </div>
                                        <div className="col-md-6">
                                            <label className="form-label">Usage Limit</label>
                                            <input type="number" className="form-control" required value={currentCoupon.usageLimit} onChange={e => setCurrentCoupon({ ...currentCoupon, usageLimit: e.target.value })} />
                                        </div>
                                    </div>
                                    <div className="row mb-3">
                                        <div className="col-md-6">
                                            <label className="form-label">Valid From</label>
                                            <input type="datetime-local" className="form-control" required value={currentCoupon.validFrom} onChange={e => setCurrentCoupon({ ...currentCoupon, validFrom: e.target.value })} />
                                        </div>
                                        <div className="col-md-6">
                                            <label className="form-label">Valid To</label>
                                            <input type="datetime-local" className="form-control" required value={currentCoupon.validTo} onChange={e => setCurrentCoupon({ ...currentCoupon, validTo: e.target.value })} />
                                        </div>
                                    </div>
                                </div>
                                <div className="modal-footer">
                                    <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
                                    <button type="submit" className="btn btn-primary">{editMode ? 'Save Changes' : 'Create'}</button>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default CouponDashboard;
