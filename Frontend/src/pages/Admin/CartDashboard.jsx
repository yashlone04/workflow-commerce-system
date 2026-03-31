import { useState, useEffect } from "react";
import cartService from "../../services/cart.service";
import authService from "../../services/auth.service";
import { useNavigate } from "react-router-dom";

const CartDashboard = () => {
    const [carts, setCarts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
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

        fetchCarts();
    }, []);

    const fetchCarts = async () => {
        try {
            setLoading(true);
            const response = await cartService.getAllCarts();
            setCarts(response.data || []);
            setError(null);
        } catch (err) {
            if (err.response?.status === 401 || err.response?.status === 403) {
                authService.logout();
                navigate("/login");
            } else {
                setError("Failed to fetch carts. Please try again.");
            }
        } finally {
            setLoading(false);
        }
    };

    const formatCurrency = (amount) => {
        return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'USD'
        }).format(amount);
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
            <h2>Cart Management</h2>

            {error && (
                <div className="alert alert-danger alert-dismissible fade show" role="alert">
                    {error}
                    <button type="button" className="btn-close" onClick={() => setError(null)}></button>
                </div>
            )}

            <div className="card">
                <div className="card-header">
                    <h5 className="mb-0">All Customer Carts</h5>
                </div>
                <div className="card-body">
                    <div className="table-responsive">
                        <table className="table table-striped table-hover">
                            <thead>
                                <tr>
                                    <th>Cart ID</th>
                                    <th>Customer</th>
                                    <th>Items Count</th>
                                    <th>Total Value</th>
                                    <th>Last Updated</th>
                                </tr>
                            </thead>
                            <tbody>
                                {carts.length === 0 ? (
                                    <tr>
                                        <td colSpan="5" className="text-center">No carts found.</td>
                                    </tr>
                                ) : (
                                    carts.map((cart) => (
                                        <tr key={cart.cartId}>
                                            <td>{cart.cartId}</td>
                                            <td>{cart.customerName}</td>
                                            <td>{cart.itemCount}</td>
                                            <td>{formatCurrency(cart.totalValue)}</td>
                                            <td>{new Date(cart.updatedAt).toLocaleString()}</td>
                                        </tr>
                                    ))
                                )}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

            <div className="card mt-4">
                <div className="card-header">
                    <h5 className="mb-0">Cart Statistics</h5>
                </div>
                <div className="card-body">
                    <div className="row">
                        <div className="col-md-4">
                            <div className="border rounded p-3 text-center">
                                <h3 className="text-primary">{carts.length}</h3>
                                <p className="mb-0">Total Carts</p>
                            </div>
                        </div>
                        <div className="col-md-4">
                            <div className="border rounded p-3 text-center">
                                <h3 className="text-success">
                                    {carts.reduce((sum, cart) => sum + cart.itemCount, 0)}
                                </h3>
                                <p className="mb-0">Total Items</p>
                            </div>
                        </div>
                        <div className="col-md-4">
                            <div className="border rounded p-3 text-center">
                                <h3 className="text-info">
                                    {formatCurrency(carts.reduce((sum, cart) => sum + cart.totalValue, 0))}
                                </h3>
                                <p className="mb-0">Total Value</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default CartDashboard;
