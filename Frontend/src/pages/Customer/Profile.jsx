import React, { useState, useEffect } from "react";
import userService from "../../services/user.service";
import AuthService from "../../services/auth.service";
import reviewService from "../../services/review.service";
import { useNavigate } from "react-router-dom";

const Profile = () => {
    const [userData, setUserData] = useState(null);
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState("");
    const [user, setUser] = useState(null);
    const [reviews, setReviews] = useState([]);
    const [editReviewData, setEditReviewData] = useState(null);
    const navigate = useNavigate();

    // Check if user is authenticated
    useEffect(() => {
        const currentUser = AuthService.getCurrentUser();
        setUser(currentUser);

        if (!currentUser?.id || !currentUser?.token) {
            setMessage("Please log in to view your profile");
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
            fetchUserProfile();
        } catch (error) {
            setMessage("Invalid session. Please log in again.");
            setTimeout(() => navigate("/login"), 1000);
        }
    }, []);

    const fetchUserProfile = async () => {
        try {
            setLoading(true);
            const res = await userService.getCurrentUser();
            setUserData(res.data);
        } catch (err) {
            console.error("Failed to fetch profile", err);
            if (err.response?.status === 403) {
                setMessage("Your session has expired. Please log in again.");
                setTimeout(() => navigate("/login"), 2000);
            } else {
                setMessage("Failed to load profile");
            }
        } finally {
            setLoading(false);
        }
    };

    const fetchMyReviews = async () => {
        try {
            const res = await reviewService.getMyReviews();
            setReviews(res.data);
        } catch (err) {
            console.error(err);
        }
    };

    useEffect(() => {
        if (user) {
            fetchMyReviews();
        }
    }, [user]);

    const handleEditSave = async () => {
        try {
            await reviewService.updateReview(editReviewData.id, editReviewData.rating, editReviewData.reviewText);
            setEditReviewData(null);
            setMessage("Review updated successfully and is pending approval.");
            fetchMyReviews();
        } catch (err) {
            setMessage(err.response?.data?.error || "Error updating review");
        }
    };

    const handleDeleteReview = async (id) => {
        if (!window.confirm("Delete this review?")) return;
        try {
            await reviewService.deleteReview(id);
            setMessage("Review deleted.");
            fetchMyReviews();
        } catch (err) {
            setMessage("Error deleting review.");
        }
    };

    // Check if user is authenticated before showing content
    if (!user) {
        return (
            <div className="container py-5">
                <div className="admin-card p-5 text-center shadow-sm">
                    <h2 className="text-danger fw-bold mb-3">Authentication Required</h2>
                    <p className="text-secondary mb-4">Please log in to view your profile.</p>
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
                    <p className="mt-3">Loading your profile...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="container py-5">
            <h2 className="mb-4">My Profile</h2>
            {message && <div className="alert alert-info">{message}</div>}

            {userData ? (
                <div className="admin-card p-4 shadow-sm">
                    <div className="row">
                        <div className="col-md-6 mb-3">
                            <label className="form-label fw-bold">Username</label>
                            <p className="form-control-static">{userData.username}</p>
                        </div>
                        <div className="col-md-6 mb-3">
                            <label className="form-label fw-bold">Email</label>
                            <p className="form-control-static">{userData.email}</p>
                        </div>
                    </div>
                    <div className="row">
                        <div className="col-md-6 mb-3">
                            <label className="form-label fw-bold">First Name</label>
                            <p className="form-control-static">{userData.firstName || 'Not provided'}</p>
                        </div>
                        <div className="col-md-6 mb-3">
                            <label className="form-label fw-bold">Last Name</label>
                            <p className="form-control-static">{userData.lastName || 'Not provided'}</p>
                        </div>
                    </div>
                    <div className="row">
                        <div className="col-md-6 mb-3">
                            <label className="form-label fw-bold">Phone Number</label>
                            <p className="form-control-static">{userData.phoneNumber || 'Not provided'}</p>
                        </div>
                        <div className="col-md-6 mb-3">
                            <label className="form-label fw-bold">Account Status</label>
                            <p className="form-control-static">
                                <span className={`badge ${userData.status ? 'bg-success' : 'bg-danger'}`}>
                                    {userData.status ? 'Active' : 'Inactive'}
                                </span>
                            </p>
                        </div>
                    </div>
                    <div className="row">
                        <div className="col-md-6 mb-3">
                            <label className="form-label fw-bold">Registration Date</label>
                            <p className="form-control-static">
                                {userData.createdAt ? new Date(userData.createdAt).toLocaleDateString() : 'N/A'}
                            </p>
                        </div>
                        <div className="col-md-6 mb-3">
                            <label className="form-label fw-bold">Roles</label>
                            <p className="form-control-static">
                                {userData.roles?.map(role => (
                                    <span key={role} className="badge bg-primary me-1">
                                        {role.replace('ROLE_', '')}
                                    </span>
                                ))}
                            </p>
                        </div>
                    </div>
                </div>
            ) : (
                <div className="admin-card p-5 text-center shadow-sm">
                    <h4 className="text-secondary mb-3">Profile Not Available</h4>
                    <p className="text-muted">Unable to load your profile information.</p>
                </div>
            )}

            {userData && (
                <div className="mt-5">
                    <h3 className="mb-4">My Reviews</h3>
                    {reviews.length === 0 ? (
                        <p className="text-muted">You have not written any reviews yet.</p>
                    ) : (
                        <div className="row">
                            {reviews.map(r => (
                                <div key={r.id} className="col-md-6 mb-4">
                                    <div className="card shadow-sm h-100">
                                        <div className="card-body">
                                            <h5 className="card-title fw-bold">{r.productName}</h5>
                                            <p className="text-warning mb-1">{"★".repeat(r.rating)}{"☆".repeat(5 - r.rating)}</p>
                                            <p className="card-text text-muted">{r.reviewText}</p>
                                            <div className="mb-3">
                                                <span className={`badge ${r.status ? 'bg-success' : 'bg-warning'}`}>
                                                    {r.status ? 'Approved' : 'Pending'}
                                                </span>
                                            </div>
                                            <div className="d-flex gap-2">
                                                <button className="btn btn-sm btn-outline-primary" onClick={() => setEditReviewData({ ...r })}>Edit</button>
                                                <button className="btn btn-sm btn-outline-danger" onClick={() => handleDeleteReview(r.id)}>Delete</button>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            )}

            {/* Edit Review Modal */}
            {editReviewData && (
                <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
                    <div className="modal-dialog modal-dialog-centered">
                        <div className="modal-content">
                            <div className="modal-header">
                                <h5 className="modal-title">Edit Review</h5>
                                <button type="button" className="btn-close" onClick={() => setEditReviewData(null)}></button>
                            </div>
                            <div className="modal-body">
                                <div className="mb-3">
                                    <label className="form-label">Rating</label>
                                    <select className="form-select" value={editReviewData.rating} onChange={(e) => setEditReviewData({ ...editReviewData, rating: Number(e.target.value) })}>
                                        <option value="5">5</option>
                                        <option value="4">4</option>
                                        <option value="3">3</option>
                                        <option value="2">2</option>
                                        <option value="1">1</option>
                                    </select>
                                </div>
                                <div className="mb-3">
                                    <label className="form-label">Text</label>
                                    <textarea className="form-control" rows="3" value={editReviewData.reviewText} onChange={(e) => setEditReviewData({ ...editReviewData, reviewText: e.target.value })}></textarea>
                                </div>
                            </div>
                            <div className="modal-footer">
                                <button className="btn btn-secondary" onClick={() => setEditReviewData(null)}>Cancel</button>
                                <button className="btn btn-primary" onClick={handleEditSave}>Save Changes</button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Profile;
