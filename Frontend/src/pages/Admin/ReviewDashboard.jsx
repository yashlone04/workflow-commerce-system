import React, { useState, useEffect } from "react";
import reviewService from "../../services/review.service";

const ReviewDashboard = () => {
    const [reviews, setReviews] = useState([]);
    const [loading, setLoading] = useState(true);
    const [message, setMessage] = useState("");

    useEffect(() => {
        fetchReviews();
    }, []);

    const fetchReviews = async () => {
        try {
            setLoading(true);
            const res = await reviewService.getAllReviews();
            setReviews(res.data);
        } catch (err) {
            setMessage("Failed to fetch reviews");
        } finally {
            setLoading(false);
        }
    };

    const handleApprove = async (reviewId) => {
        try {
            await reviewService.approveReview(reviewId);
            fetchReviews();
        } catch (err) {
            setMessage(err.response?.data?.error || "Error approving review");
        }
    };

    const handleReject = async (reviewId) => {
        try {
            await reviewService.rejectReview(reviewId);
            fetchReviews();
        } catch (err) {
            setMessage(err.response?.data?.error || "Error rejecting review");
        }
    };

    const handleDelete = async (reviewId) => {
        if (!window.confirm("Delete this review entirely?")) return;
        try {
            await reviewService.deleteReview(reviewId);
            fetchReviews();
        } catch (err) {
            setMessage(err.response?.data?.error || "Error deleting review");
        }
    };

    if (loading) return <div className="p-5 text-center h4">Loading...</div>;

    return (
        <div className="container py-5">
            <h2 className="mb-4 text-primary fw-bold">Review Moderation</h2>
            {message && <div className="alert alert-info">{message}</div>}

            <div className="card shadow-sm border-0">
                <div className="card-body p-0">
                    <div className="table-responsive">
                        <table className="table table-hover align-middle mb-0">
                            <thead className="table-light">
                                <tr>
                                    <th>ID</th>
                                    <th>Product</th>
                                    <th>Customer</th>
                                    <th>Rating</th>
                                    <th>Text</th>
                                    <th>Status</th>
                                    <th className="text-end">Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {reviews.map(r => (
                                    <tr key={r.id}>
                                        <td>{r.id}</td>
                                        <td className="fw-bold">{r.productName}</td>
                                        <td>{r.customerName}</td>
                                        <td className="text-warning">{"★".repeat(r.rating)}</td>
                                        <td><small>{r.reviewText}</small></td>
                                        <td>
                                            <span className={`badge ${r.status ? 'bg-success' : 'bg-warning'}`}>
                                                {r.status ? 'Approved' : 'Pending'}
                                            </span>
                                        </td>
                                        <td className="text-end">
                                            {!r.status ? (
                                                <button onClick={() => handleApprove(r.id)} className="btn btn-sm btn-success me-1">Approve</button>
                                            ) : (
                                                <button onClick={() => handleReject(r.id)} className="btn btn-sm btn-warning me-1">Reject</button>
                                            )}
                                            <button onClick={() => handleDelete(r.id)} className="btn btn-sm btn-danger">Delete</button>
                                        </td>
                                    </tr>
                                ))}
                                {reviews.length === 0 && (
                                    <tr><td colSpan="7" className="text-center py-4">No reviews found in the system.</td></tr>
                                )}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ReviewDashboard;
