import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import publicCategoryService from "../../services/publicCategory.service";
import publicProductService from "../../services/publicProduct.service";
import cartService from "../../services/cart.service";
import wishlistService from "../../services/wishlist.service";
import authService from "../../services/auth.service";
import reviewService from "../../services/review.service";

const ProductCatalog = () => {
    const [products, setProducts] = useState([]);
    const [categories, setCategories] = useState([]);
    const [selectedCategory, setSelectedCategory] = useState("all");
    const [loading, setLoading] = useState(true);
    const [cartCount, setCartCount] = useState(0);
    const [wishlistItems, setWishlistItems] = useState([]);
    const [message, setMessage] = useState("");

    // Reviews state
    const [showReviewsModal, setShowReviewsModal] = useState(false);
    const [selectedProductForReviews, setSelectedProductForReviews] = useState(null);
    const [productReviews, setProductReviews] = useState({ averageRating: 0, totalReviews: 0, reviews: [] });
    const [newReviewRating, setNewReviewRating] = useState(5);
    const [newReviewText, setNewReviewText] = useState("");
    const [reviewMessage, setReviewMessage] = useState("");

    const navigate = useNavigate();

    useEffect(() => {
        fetchCategories();
        fetchProducts();
        fetchCartCount();
        fetchWishlistItems();
    }, []);

    const fetchCartCount = async () => {
        const currentUser = authService.getCurrentUser();
        if (!currentUser) return;

        try {
            const response = await cartService.getMyCart();
            setCartCount(response.data.itemCount || 0);
        } catch (err) {
            // Silently ignore cart fetch errors
        }
    };

    const fetchCategories = async () => {
        try {
            const res = await publicCategoryService.getAll();
            setCategories(res.data.filter(cat => cat.status));
        } catch (err) {
            console.error("Failed to load categories", err);
        }
    };

    const fetchProducts = async () => {
        try {
            setLoading(true);
            const res = await publicProductService.getAll();
            // Filter only active products
            const activeProducts = Array.isArray(res.data) ? res.data : [];
            setProducts(activeProducts);
        } catch (err) {
            console.error("Failed to load products", err);
            setProducts([]);
        } finally {
            setLoading(false);
        }
    };


    const filteredProducts = selectedCategory === "all"
        ? products
        : products.filter(product => product.category?.category_id === parseInt(selectedCategory));

    const groupedProducts = categories.reduce((acc, category) => {
        const categoryProducts = filteredProducts.filter(
            product => product.category?.category_id === category.category_id
        );
        if (categoryProducts.length > 0) {
            acc[category.category_name] = categoryProducts;
        }
        return acc;
    }, {});

    const handleAddToCart = async (product) => {
        const currentUser = authService.getCurrentUser();
        if (!currentUser) {
            navigate("/login");
            return;
        }

        try {
            await cartService.addToCart(product.productId, 1);
            setMessage(`${product.productName} added to cart!`);
            fetchCartCount();
            setTimeout(() => setMessage(""), 2000);
        } catch (err) {
            setMessage(err.response?.data?.message || "Failed to add to cart");
            setTimeout(() => setMessage(""), 3000);
        }
    };

    const fetchWishlistItems = async () => {
        const currentUser = authService.getCurrentUser();
        if (!currentUser) return;

        try {
            const response = await wishlistService.getMyWishlist();
            setWishlistItems(response.data || []);
        } catch (err) {
            // Silently ignore wishlist fetch errors
        }
    };

    const isProductInWishlist = (productId) => {
        return wishlistItems.some(item => item.productId === productId);
    };

    const handleAddToWishlist = async (product) => {
        const currentUser = authService.getCurrentUser();
        if (!currentUser) {
            navigate("/login");
            return;
        }

        try {
            await wishlistService.addToWishlist(product.productId);
            setMessage(`${product.productName} added to wishlist!`);
            fetchWishlistItems();
            setTimeout(() => setMessage(""), 2000);
        } catch (err) {
            setMessage(err.response?.data?.message || "Failed to add to wishlist");
            setTimeout(() => setMessage(""), 3000);
        }
    };

    const handleShowReviews = async (product) => {
        setSelectedProductForReviews(product);
        setShowReviewsModal(true);
        setReviewMessage("");
        setNewReviewText("");
        setNewReviewRating(5);
        try {
            const res = await reviewService.getProductReviews(product.productId);
            setProductReviews(res.data);
        } catch (err) {
            console.error("Failed to load reviews", err);
        }
    };

    const submitReview = async () => {
        try {
            await reviewService.addReview(selectedProductForReviews.productId, newReviewRating, newReviewText);
            setReviewMessage("Review submitted! It is pending approval.");
            setNewReviewText("");
            setNewReviewRating(5);
            // Refresh reviews
            const res = await reviewService.getProductReviews(selectedProductForReviews.productId);
            setProductReviews(res.data);
        } catch (err) {
            setReviewMessage(err.response?.data?.error || "Failed to submit review. Ensure order is delivered.");
        }
    };

    if (loading) {
        return (
            <div className="container py-5">
                <div className="text-center">
                    <div className="spinner-border text-primary" role="status">
                        <span className="visually-hidden">Loading...</span>
                    </div>
                    <p className="mt-3">Loading products...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="container py-5">
            {message && (
                <div className="alert alert-info alert-dismissible fade show" role="alert">
                    {message}
                    <button type="button" className="btn-close" onClick={() => setMessage("")}></button>
                </div>
            )}
            <div className="row mb-4">
                <div className="col-12">
                    <h2 className="mb-4">Product Catalog</h2>

                    {/* Category Filter */}
                    <div className="mb-4">
                        <label htmlFor="categoryFilter" className="form-label fw-bold">
                            Filter by Category:
                        </label>
                        <select
                            id="categoryFilter"
                            className="form-select"
                            value={selectedCategory}
                            onChange={(e) => setSelectedCategory(e.target.value)}
                        >
                            <option value="all">All Categories</option>
                            {categories.map((category) => (
                                <option key={category.category_id} value={category.category_id}>
                                    {category.category_name}
                                </option>
                            ))}
                        </select>
                    </div>
                </div>
            </div>

            {Object.keys(groupedProducts).length === 0 ? (
                <div className="text-center py-5">
                    <div className="admin-card p-5 d-inline-block shadow-sm">
                        <h4 className="text-secondary mb-3">No Products Available</h4>
                        <p className="text-muted">
                            {selectedCategory === "all"
                                ? "There are currently no active products in the catalog."
                                : "There are no products in this category."
                            }
                        </p>
                    </div>
                </div>
            ) : (
                Object.entries(groupedProducts).map(([categoryName, categoryProducts]) => (
                    <div key={categoryName} className="mb-5">
                        <h3 className="mb-4 border-bottom pb-2">
                            {categoryName}
                            <span className="badge bg-secondary ms-2">{categoryProducts.length}</span>
                        </h3>

                        <div className="row">
                            {categoryProducts.map((product) => (
                                <div key={product.productId} className="col-md-6 col-lg-4 mb-4">
                                    <div className="card h-100 shadow-sm">
                                        <div className="card-body">
                                            <h5 className="card-title">{product.productName}</h5>
                                            <p className="card-text text-muted small">
                                                SKU: {product.sku}
                                            </p>
                                            <p className="card-text">
                                                {product.description || "No description available"}
                                            </p>
                                            <div className="d-flex justify-content-between align-items-center mb-3">
                                                <span className="h5 text-primary mb-0">
                                                    ${product.price}
                                                </span>
                                                <span className={`badge ${product.inventoryCount > 10 ? 'bg-success' : product.inventoryCount > 0 ? 'bg-warning' : 'bg-danger'}`}>
                                                    {product.inventoryCount > 0 ? `In Stock (${product.inventoryCount})` : 'Out of Stock'}
                                                </span>
                                            </div>
                                            <div className="d-flex gap-2 flex-wrap">
                                                <button
                                                    className="btn-primary-tech btn-sm flex-fill"
                                                    onClick={() => handleAddToCart(product)}
                                                    disabled={product.inventoryCount === 0}
                                                >
                                                    Add to Cart
                                                </button>
                                                <button
                                                    className="btn-outline-tech btn-sm"
                                                    onClick={() => navigate('/cart')}
                                                >
                                                    View Cart ({cartCount})
                                                </button>
                                                <button
                                                    className={`btn-sm flex-fill ${isProductInWishlist(product.productId) ? 'btn-secondary' : 'btn-outline-primary'}`}
                                                    onClick={() => handleAddToWishlist(product)}
                                                    disabled={isProductInWishlist(product.productId)}
                                                >
                                                    {isProductInWishlist(product.productId) ? 'In Wishlist' : 'Add to Wishlist'}
                                                </button>
                                            </div>
                                            <button
                                                className="btn btn-outline-info btn-sm w-100 mt-2"
                                                onClick={() => handleShowReviews(product)}
                                            >
                                                Reviews
                                            </button>
                                        </div>
                                        <div className="card-footer bg-light">
                                            <small className="text-muted">
                                                Category: {product.category?.category_name}
                                            </small>
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                ))
            )}

            {/* Reviews Modal */}
            {showReviewsModal && selectedProductForReviews && (
                <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
                    <div className="modal-dialog modal-dialog-centered modal-lg">
                        <div className="modal-content">
                            <div className="modal-header">
                                <h5 className="modal-title">Reviews for {selectedProductForReviews.productName}</h5>
                                <button type="button" className="btn-close" onClick={() => setShowReviewsModal(false)}></button>
                            </div>
                            <div className="modal-body" style={{ maxHeight: '600px', overflowY: 'auto' }}>
                                {reviewMessage && <div className="alert alert-info">{reviewMessage}</div>}

                                <div className="d-flex align-items-center mb-4">
                                    <h4 className="me-3 mb-0">{productReviews.averageRating} / 5</h4>
                                    <span className="text-muted">({productReviews.totalReviews} total approved reviews)</span>
                                </div>

                                <div className="mb-4">
                                    {productReviews.reviews.length > 0 ? (
                                        productReviews.reviews.map(review => (
                                            <div key={review.id} className="border p-3 mb-2 rounded bg-light">
                                                <div className="d-flex justify-content-between mb-2">
                                                    <strong className="text-primary">{review.customerName}</strong>
                                                    <span className="text-warning">{"★".repeat(review.rating)}{"☆".repeat(5 - review.rating)}</span>
                                                </div>
                                                <p className="mb-1">{review.reviewText}</p>
                                                <small className="text-muted">{new Date(review.createdAt).toLocaleDateString()}</small>
                                            </div>
                                        ))
                                    ) : (
                                        <p className="text-muted">No reviews yet for this product.</p>
                                    )}
                                </div>

                                {authService.getCurrentUser() && (
                                    <div className="card shadow-sm border-0">
                                        <div className="card-header bg-primary text-white">Write a Review</div>
                                        <div className="card-body">
                                            <div className="mb-3">
                                                <label className="form-label">Rating (1-5)</label>
                                                <select className="form-select" value={newReviewRating} onChange={e => setNewReviewRating(Number(e.target.value))}>
                                                    <option value="5">5 - Excellent</option>
                                                    <option value="4">4 - Very Good</option>
                                                    <option value="3">3 - Good</option>
                                                    <option value="2">2 - Fair</option>
                                                    <option value="1">1 - Poor</option>
                                                </select>
                                            </div>
                                            <div className="mb-3">
                                                <label className="form-label">Review Text</label>
                                                <textarea
                                                    className="form-control"
                                                    rows="3"
                                                    maxLength="1000"
                                                    value={newReviewText}
                                                    onChange={e => setNewReviewText(e.target.value)}
                                                    placeholder="Share your experience..."
                                                ></textarea>
                                            </div>
                                            <button className="btn btn-success w-100" onClick={submitReview}>Submit Review</button>
                                        </div>
                                    </div>
                                )}
                            </div>
                            <div className="modal-footer">
                                <button type="button" className="btn btn-secondary" onClick={() => setShowReviewsModal(false)}>Close</button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default ProductCatalog;
