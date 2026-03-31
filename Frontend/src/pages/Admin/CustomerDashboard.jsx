import React, { useState, useEffect, useRef } from "react";
import userService from "../../services/user.service";
import AuthService from "../../services/auth.service";
import { useNavigate } from "react-router-dom";

let isFetchingUsers = false;

const CustomerDashboard = () => {
    const [users, setUsers] = useState([]);
    const [filteredUsers, setFilteredUsers] = useState([]);
    const [statusFilter, setStatusFilter] = useState("all");
    const [emailFilter, setEmailFilter] = useState("");
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState("");
    const [user, setUser] = useState(null);
    const [showEditModal, setShowEditModal] = useState(false);
    const [showDeactivateModal, setShowDeactivateModal] = useState(false);
    const [selectedUser, setSelectedUser] = useState(null);
    const [editForm, setEditForm] = useState({
        firstName: "",
        lastName: "",
        email: "",
        phoneNumber: "",
        status: true,
    });
    const navigate = useNavigate();
    const hasFetched = useRef(false);

    // Check if user is authenticated as admin
    useEffect(() => {
        const currentUser = AuthService.getCurrentUser();
        setUser(currentUser);
    }, []);

    useEffect(() => {
        if (hasFetched.current) return;

        if (!user?.id || !user?.token) return;

        if (user.roles?.includes("ROLE_ADMIN")) {
            try {
                const tokenPayload = JSON.parse(atob(user.token.split('.')[1]));
                if (tokenPayload.exp < Date.now() / 1000) {
                    setMessage("Session expired. Please log in again.");
                    setTimeout(() => navigate("/login"), 1000);
                    return;
                }
                hasFetched.current = true;
                fetchUsers();
            } catch (error) {
                setMessage("Invalid session. Please log in again.");
                setTimeout(() => navigate("/login"), 1000);
            }
        }
    }, [user?.id]);

    useEffect(() => {
        let result = users;

        if (statusFilter !== "all") {
            const isActive = statusFilter === "active";
            result = result.filter(u => u.status === isActive);
        }

        if (emailFilter.trim()) {
            result = result.filter(u =>
                u.email?.toLowerCase().includes(emailFilter.toLowerCase())
            );
        }

        setFilteredUsers(result);
    }, [statusFilter, emailFilter, users]);

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

    const fetchUsers = async () => {
        if (loading || isFetchingUsers) return;

        isFetchingUsers = true;
        try {
            setLoading(true);
            const res = await userService.getAllUsers();
            setUsers(Array.isArray(res.data) ? res.data : []);
        } catch (err) {
            console.error("Failed to fetch users", err);
            setMessage("Failed to load users");
        } finally {
            setLoading(false);
            isFetchingUsers = false;
        }
    };

    const handleEditClick = (userData) => {
        setSelectedUser(userData);
        setEditForm({
            firstName: userData.firstName || "",
            lastName: userData.lastName || "",
            email: userData.email || "",
            phoneNumber: userData.phoneNumber || "",
            status: userData.status,
        });
        setShowEditModal(true);
    };

    const handleDeactivateClick = (userData) => {
        setSelectedUser(userData);
        setShowDeactivateModal(true);
    };

    const handleUpdateUser = async (e) => {
        e.preventDefault();
        try {
            await userService.updateUser(selectedUser.id, editForm);
            setMessage(`User ${selectedUser.username} updated successfully`);
            setShowEditModal(false);
            fetchUsers();
        } catch (err) {
            setMessage(err.response?.data?.message || "Failed to update user");
        }
    };

    const handleDeactivateUser = async () => {
        try {
            await userService.deactivateUser(selectedUser.id);
            setMessage(`User ${selectedUser.username} deactivated successfully`);
            setShowDeactivateModal(false);
            fetchUsers();
        } catch (err) {
            setMessage(err.response?.data?.message || "Failed to deactivate user");
        }
    };

    const getStatusBadge = (status) => {
        return status ? "bg-success" : "bg-danger";
    };

    if (loading) {
        return (
            <div className="container py-5">
                <div className="text-center">
                    <div className="spinner-border text-primary" role="status">
                        <span className="visually-hidden">Loading...</span>
                    </div>
                    <p className="mt-3">Loading customers...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="container py-5">
            <h2 className="mb-4">Customer Management</h2>
            {message && <div className="alert alert-info">{message}</div>}

            <div className="admin-card p-4 shadow-sm mb-4">
                <div className="row g-3">
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
                            <option value="all">All Customers</option>
                            <option value="active">Active</option>
                            <option value="inactive">Inactive</option>
                        </select>
                    </div>
                    <div className="col-md-4">
                        <label htmlFor="emailFilter" className="form-label">
                            Search by Email
                        </label>
                        <input
                            type="text"
                            id="emailFilter"
                            className="form-control"
                            placeholder="Enter email..."
                            value={emailFilter}
                            onChange={(e) => setEmailFilter(e.target.value)}
                        />
                    </div>
                    <div className="col-md-4 d-flex align-items-end">
                        <div className="text-muted small">
                            Showing {filteredUsers.length} of {users.length} customers
                        </div>
                    </div>
                </div>
            </div>

            {filteredUsers.length === 0 ? (
                <div className="admin-card p-5 text-center shadow-sm">
                    <h4 className="text-secondary mb-3">No Customers Found</h4>
                    <p className="text-muted">
                        {statusFilter !== "all" || emailFilter
                            ? "No customers match your filters."
                            : "No customers have been registered yet."
                        }
                    </p>
                </div>
            ) : (
                <div className="admin-card p-4 shadow-sm">
                    <div className="table-responsive">
                        <table className="table">
                            <thead>
                                <tr>
                                    <th>Customer ID</th>
                                    <th>First Name</th>
                                    <th>Last Name</th>
                                    <th>Email</th>
                                    <th>Phone Number</th>
                                    <th>Account Status</th>
                                    <th>Registration Date</th>
                                    <th>Orders</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {filteredUsers.map((userData) => (
                                    <tr key={userData.id}>
                                        <td>#{userData.id}</td>
                                        <td>{userData.firstName || 'N/A'}</td>
                                        <td>{userData.lastName || 'N/A'}</td>
                                        <td>{userData.email}</td>
                                        <td>{userData.phoneNumber || 'N/A'}</td>
                                        <td>
                                            <span className={`badge ${getStatusBadge(userData.status)}`}>
                                                {userData.status ? 'Active' : 'Inactive'}
                                            </span>
                                        </td>
                                        <td>{new Date(userData.createdAt).toLocaleDateString()}</td>
                                        <td>{userData.orderCount || 0}</td>
                                        <td>
                                            <div className="btn-group" role="group">
                                                <button
                                                    className="btn btn-sm btn-primary"
                                                    onClick={() => handleEditClick(userData)}
                                                    title="Edit Customer"
                                                >
                                                    Edit
                                                </button>
                                                {userData.status && (
                                                    <button
                                                        className="btn btn-sm btn-danger ms-1"
                                                        onClick={() => handleDeactivateClick(userData)}
                                                        title="Deactivate Customer"
                                                    >
                                                        Deactivate
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

            {/* Edit Modal */}
            {showEditModal && (
                <div className="modal show" style={{ display: 'block', backgroundColor: 'rgba(0,0,0,0.5)' }}>
                    <div className="modal-dialog">
                        <div className="modal-content">
                            <div className="modal-header">
                                <h5 className="modal-title">Edit Customer</h5>
                                <button type="button" className="btn-close" onClick={() => setShowEditModal(false)}></button>
                            </div>
                            <form onSubmit={handleUpdateUser}>
                                <div className="modal-body">
                                    <div className="mb-3">
                                        <label className="form-label">First Name</label>
                                        <input
                                            type="text"
                                            className="form-control"
                                            value={editForm.firstName}
                                            onChange={(e) => setEditForm({ ...editForm, firstName: e.target.value })}
                                        />
                                    </div>
                                    <div className="mb-3">
                                        <label className="form-label">Last Name</label>
                                        <input
                                            type="text"
                                            className="form-control"
                                            value={editForm.lastName}
                                            onChange={(e) => setEditForm({ ...editForm, lastName: e.target.value })}
                                        />
                                    </div>
                                    <div className="mb-3">
                                        <label className="form-label">Email</label>
                                        <input
                                            type="email"
                                            className="form-control"
                                            value={editForm.email}
                                            onChange={(e) => setEditForm({ ...editForm, email: e.target.value })}
                                            required
                                        />
                                    </div>
                                    <div className="mb-3">
                                        <label className="form-label">Phone Number</label>
                                        <input
                                            type="text"
                                            className="form-control"
                                            value={editForm.phoneNumber}
                                            onChange={(e) => setEditForm({ ...editForm, phoneNumber: e.target.value })}
                                        />
                                    </div>
                                    <div className="mb-3">
                                        <label className="form-label">Account Status</label>
                                        <select
                                            className="form-select"
                                            value={editForm.status}
                                            onChange={(e) => setEditForm({ ...editForm, status: e.target.value === 'true' })}
                                        >
                                            <option value="true">Active</option>
                                            <option value="false">Inactive</option>
                                        </select>
                                    </div>
                                </div>
                                <div className="modal-footer">
                                    <button type="button" className="btn btn-secondary" onClick={() => setShowEditModal(false)}>Cancel</button>
                                    <button type="submit" className="btn btn-primary">Save Changes</button>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            )}

            {/* Deactivate Modal */}
            {showDeactivateModal && (
                <div className="modal show" style={{ display: 'block', backgroundColor: 'rgba(0,0,0,0.5)' }}>
                    <div className="modal-dialog">
                        <div className="modal-content">
                            <div className="modal-header">
                                <h5 className="modal-title text-danger">Deactivate Customer</h5>
                                <button type="button" className="btn-close" onClick={() => setShowDeactivateModal(false)}></button>
                            </div>
                            <div className="modal-body">
                                <p>Are you sure you want to deactivate customer <strong>{selectedUser?.username}</strong>?</p>
                                <p className="text-muted small">This action will prevent the user from logging in. The account will remain in the system but will be marked as inactive.</p>
                            </div>
                            <div className="modal-footer">
                                <button type="button" className="btn btn-secondary" onClick={() => setShowDeactivateModal(false)}>Cancel</button>
                                <button type="button" className="btn btn-danger" onClick={handleDeactivateUser}>Deactivate</button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default CustomerDashboard;
