import React, { useState, useEffect } from "react";
import CategoryService from "../../services/category.service";
import AuthService from "../../services/auth.service";
import { useNavigate } from "react-router-dom";

const CategoryDashboard = () => {
    const [categories, setCategories] = useState([]);
    const [user, setUser] = useState(null);
    const [showModal, setShowModal] = useState(false);
    const [editMode, setEditMode] = useState(false);
    const [currentCategory, setCurrentCategory] = useState({ category_id: null, category_name: "", description: "" });
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();
    const [hasFetched, setHasFetched] = useState(false);

    // Check if user is authenticated as admin
    useEffect(() => {
        const currentUser = AuthService.getCurrentUser();
        setUser(currentUser);
    }, []);

    useEffect(() => {
        if (hasFetched || !user?.id || !user?.token) return;

        if (user.roles?.includes("ROLE_ADMIN")) {
            setHasFetched(true);
            loadCategories();
        }
    }, [user?.id, hasFetched]);

    const loadCategories = () => {
        if (loading) {
            console.log('CategoryDashboard: loadCategories called while loading, skipping');
            return;
        }

        console.log('CategoryDashboard: loadCategories called');

        setLoading(true);
        CategoryService.getAllCategories().then(
            (response) => {
                setCategories(response.data);
                setLoading(false);
                console.log('CategoryDashboard: loadCategories completed successfully');
            },
            (err) => {
                console.error("Error fetching categories", err);
                setError("Failed to load categories. Please try again later.");
                setLoading(false);
            }
        );
    };

    const handleOpenModal = (category = { category_id: null, category_name: "", description: "" }) => {
        setCurrentCategory(category);
        setEditMode(!!category.category_id);
        setShowModal(true);
        setMessage("");
        setError("");
    };

    const handleCloseModal = () => {
        setShowModal(false);
        setMessage("");
        setError("");
    };

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setCurrentCategory({ ...currentCategory, [name]: value });
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        setError("");

        if (!currentCategory.category_name.trim()) {
            setError("Category name is required");
            return;
        }

        const apiCall = editMode
            ? CategoryService.updateCategory(currentCategory.category_id, currentCategory.category_name, currentCategory.description)
            : CategoryService.createCategory(currentCategory.category_name, currentCategory.description);

        apiCall.then(
            () => {
                const successMsg = editMode ? "Category updated successfully" : "Category created successfully";
                setMessage(successMsg);
                loadCategories();
                setTimeout(() => {
                    handleCloseModal();
                }, 1000);
            },
            (err) => {
                const errMsg = err.response?.data?.message || "An error occurred.";
                setError(errMsg);
            }
        );
    };

    const handleDeactivate = (id) => {
        if (window.confirm("Please assign products to another category before deactivating.")) {
            CategoryService.deactivateCategory(id).then(
                () => {
                    setMessage("Category deactivated successfully");
                    loadCategories();
                    setTimeout(() => setMessage(""), 3000);
                },
                (err) => {
                    setError("Error deactivating category");
                }
            );
        }
    };

    // Early return after all hooks
    if (!user || !user.roles?.includes("ROLE_ADMIN")) {
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

    return (
        <div className="py-4 animate-fade-in">
            <div className="d-flex justify-content-between align-items-center mb-4">
                <div>
                    <h2 className="mb-1">Category Management</h2>
                    <p className="text-secondary small mb-0">Centralized taxonomy management and catalog organization</p>
                </div>
                <button className="btn-primary-tech shadow-sm" onClick={() => handleOpenModal()}>
                    + Add Category
                </button>
            </div>

            {message && (
                <div className="alert alert-success border-0 shadow-sm py-2 px-3 small d-flex justify-content-between align-items-center mb-4">
                    <span>{message}</span>
                    <button type="button" className="btn-close small" onClick={() => setMessage("")}></button>
                </div>
            )}

            {error && (
                <div className="alert alert-danger border-0 shadow-sm py-2 px-3 small d-flex justify-content-between align-items-center mb-4">
                    <span>{error}</span>
                    <button type="button" className="btn-close small" onClick={() => setError("")}></button>
                </div>
            )}

            <div className="admin-card overflow-hidden">
                <div className="table-responsive">
                    <table className="table-bigtech mb-0">
                        <thead>
                            <tr>
                                <th>Category Name</th>
                                <th>Description</th>
                                <th>Status</th>
                                <th>Product Count</th>
                                <th className="text-end">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {loading ? (
                                <tr>
                                    <td colSpan="5" className="text-center py-5">
                                        <div className="spinner-border spinner-border-sm text-primary me-2"></div>
                                        <span className="text-secondary">Loading taxonomic data...</span>
                                    </td>
                                </tr>
                            ) : categories.length === 0 ? (
                                <tr>
                                    <td colSpan="5" className="text-center py-5 text-secondary">
                                        No categories found. Click "Add Category" to get started.
                                    </td>
                                </tr>
                            ) : (
                                categories.map((cat) => (
                                    <tr key={cat.category_id}>
                                        <td className="fw-medium text-dark">{cat.category_name}</td>
                                        <td className="text-secondary small">
                                            {cat.description || <span className="text-muted italic">No description</span>}
                                        </td>
                                        <td>
                                            <span className={`status-badge ${cat.status ? 'status-active' : 'status-inactive'}`}>
                                                {cat.status ? 'Active' : 'Inactive'}
                                            </span>
                                        </td>
                                        <td>
                                            <span className="badge bg-light text-secondary border px-2 py-1">
                                                {cat.productCount || 0}
                                            </span>
                                        </td>
                                        <td className="text-end">
                                            <div className="btn-group btn-group-sm">
                                                <button className="btn btn-outline-primary btn-sm" onClick={() => handleOpenModal(cat)}>
                                                    Edit
                                                </button>
                                                <button className="btn btn-outline-danger btn-sm" onClick={() => handleDeactivate(cat.category_id)}>
                                                    Deactivate
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* Modal */}
            {showModal && (
                <div className="modal show d-block" tabIndex="-1">
                    <div className="modal-dialog">
                        <div className="modal-content">
                            <div className="modal-header">
                                <h5 className="modal-title">{editMode ? 'Edit Category' : 'Add Category'}</h5>
                                <button type="button" className="btn-close" onClick={handleCloseModal}></button>
                            </div>
                            <div className="modal-body">
                                <form onSubmit={handleSubmit}>
                                    <div className="mb-3">
                                        <label htmlFor="category_name" className="form-label">Category Name</label>
                                        <input
                                            type="text"
                                            className="form-control"
                                            id="category_name"
                                            name="category_name"
                                            value={currentCategory.category_name}
                                            onChange={handleInputChange}
                                            required
                                        />
                                    </div>
                                    <div className="mb-3">
                                        <label htmlFor="description" className="form-label">Description</label>
                                        <textarea
                                            className="form-control"
                                            id="description"
                                            name="description"
                                            value={currentCategory.description}
                                            onChange={handleInputChange}
                                            rows="3"
                                        />
                                    </div>
                                    <div className="modal-footer">
                                        <button type="button" className="btn btn-secondary" onClick={handleCloseModal}>
                                            Cancel
                                        </button>
                                        <button type="submit" className="btn-primary-tech">
                                            {editMode ? 'Update' : 'Create'}
                                        </button>
                                    </div>
                                </form>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default CategoryDashboard;
