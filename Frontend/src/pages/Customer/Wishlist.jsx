import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import wishlistService from "../../services/wishlist.service";
import AuthService from "../../services/auth.service";

const Wishlist = () => {
    const [wishlistItems, setWishlistItems] = useState([]);
    const [loading, setLoading] = useState(true);
    const [message, setMessage] = useState("");
    const [user, setUser] = useState(null);
    const navigate = useNavigate();

    useEffect(() => {
        const currentUser = AuthService.getCurrentUser();
        setUser(currentUser);

        if (!currentUser) {
            navigate("/login");
            return;
        }

        fetchWishlist();
    }, []);

    const fetchWishlist = async () => {
        try {
            setLoading(true);
            const response = await wishlistService.getMyWishlist();
            setWishlistItems(response.data || []);
        } catch (err) {
            if (err.response?.status === 401 || err.response?.status === 403) {
                AuthService.logout();
                navigate("/login");
            } else {
                setMessage("Failed to load wishlist");
            }
        } finally {
            setLoading(false);
        }
    };

    const handleRemoveItem = async (itemId) => {
        if (!window.confirm("Remove this item from wishlist?")) return;

        try {
            await wishlistService.removeFromWishlist(itemId);
            setMessage("Item removed from wishlist");
            fetchWishlist();
            setTimeout(() => setMessage(""), 2000);
        } catch (err) {
            setMessage(err.response?.data?.message || "Failed to remove item");
        }
    };

    const handleMoveToCart = async (itemId) => {
        try {
            await wishlistService.moveToCart(itemId);
            setMessage("Item moved to cart successfully");
            fetchWishlist();
            setTimeout(() => setMessage(""), 2000);
        } catch (err) {
            setMessage(err.response?.data?.message || "Failed to move item to cart");
        }
    };

    const formatCurrency = (amount) => {
        return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'USD'
        }).format(amount);
    };

    const formatDate = (dateString) => {
        if (!dateString) return "-";
        return new Date(dateString).toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });
    };

    if (loading) {
        return (
            <div className="container py-5 text-center">
                <div className="spinner-border" role="status"></div>
            </div>
        );
    }

    return (
        <div className="container py-5">
            <h2 className="mb-4">My Wishlist</h2>

            {message && (
                <div className="alert alert-info alert-dismissible fade show" role="alert">
                    {message}
                    <button type="button" className="btn-close" onClick={() => setMessage("")}></button>
                </div>
            )}

            {wishlistItems.length === 0 ? (
                <div className="admin-card p-5 text-center shadow-sm">
                    <h4 className="text-secondary mb-3">Your wishlist is empty</h4>
                    <p className="text-muted">Browse products and add items to your wishlist</p>
                    <button className="btn-primary-tech" onClick={() => navigate("/products")}>
                        Browse Products
                    </button>
                </div>
            ) : (
                <div className="admin-card p-4 shadow-sm">
                    <div className="table-responsive">
                        <table className="table">
                            <thead>
                                <tr>
                                    <th>Product Name</th>
                                    <th>Price</th>
                                    <th>Availability</th>
                                    <th>Added Date</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {wishlistItems.map((item) => (
                                    <tr key={item.id}>
                                        <td>
                                            <div>
                                                <h6 className="mb-0">{item.productName}</h6>
                                                <small className="text-muted">ID: {item.productId}</small>
                                            </div>
                                        </td>
                                        <td>{formatCurrency(item.price)}</td>
                                        <td>
                                            {item.inStock ? (
                                                <span className="badge bg-success">In Stock</span>
                                            ) : (
                                                <span className="badge bg-danger">Out of Stock</span>
                                            )}
                                        </td>
                                        <td>{formatDate(item.addedAt)}</td>
                                        <td>
                                            <div className="d-flex gap-2">
                                                <button
                                                    className="btn btn-sm btn-primary"
                                                    onClick={() => handleMoveToCart(item.id)}
                                                    disabled={!item.inStock}
                                                    title={item.inStock ? "Move to Cart" : "Out of stock"}
                                                >
                                                    Move to Cart
                                                </button>
                                                <button
                                                    className="btn btn-sm btn-danger"
                                                    onClick={() => handleRemoveItem(item.id)}
                                                >
                                                    Remove
                                                </button>
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

export default Wishlist;
