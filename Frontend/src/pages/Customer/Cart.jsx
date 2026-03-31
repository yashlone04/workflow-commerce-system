import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import cartService from "../../services/cart.service";
import orderService from "../../services/order.service";
import AuthService from "../../services/auth.service";
import couponService from "../../services/coupon.service";

const Cart = () => {
    const [cartItems, setCartItems] = useState([]);
    const [totalAmount, setTotalAmount] = useState(0);
    const [shippingAddress, setShippingAddress] = useState("");
    const [loading, setLoading] = useState(true);
    const [message, setMessage] = useState("");
    const [user, setUser] = useState(null);
    const [updatingItem, setUpdatingItem] = useState(null);

    // Coupon state
    const [couponCode, setCouponCode] = useState("");
    const [appliedCoupon, setAppliedCoupon] = useState(null);
    const [discountAmount, setDiscountAmount] = useState(0);
    const [finalTotal, setFinalTotal] = useState(0);
    const [couponMessage, setCouponMessage] = useState("");

    const navigate = useNavigate();

    useEffect(() => {
        const currentUser = AuthService.getCurrentUser();
        setUser(currentUser);

        if (!currentUser) {
            navigate("/login");
            return;
        }

        fetchCart();
    }, []);

    const fetchCart = async () => {
        try {
            setLoading(true);
            const response = await cartService.getMyCart();
            setCartItems(response.data.items || []);
            setTotalAmount(response.data.totalAmount || 0);
            setFinalTotal(response.data.totalAmount || 0);

            // Reset coupon when cart changes
            setAppliedCoupon(null);
            setDiscountAmount(0);
            setCouponCode("");
            setCouponMessage("");
        } catch (err) {
            if (err.response?.status === 401 || err.response?.status === 403) {
                AuthService.logout();
                navigate("/login");
            } else {
                setMessage("Failed to load cart");
            }
        } finally {
            setLoading(false);
        }
    };

    const handleQuantityChange = async (itemId, newQuantity) => {
        if (newQuantity < 1) return;

        try {
            setUpdatingItem(itemId);
            await cartService.updateCartItem(itemId, newQuantity);
            fetchCart();
        } catch (err) {
            setMessage(err.response?.data?.message || "Failed to update quantity");
        } finally {
            setUpdatingItem(null);
        }
    };

    const handleRemoveItem = async (itemId) => {
        if (!window.confirm("Remove this item from cart?")) return;

        try {
            await cartService.removeFromCart(itemId);
            setMessage("Item removed");
            fetchCart();
        } catch (err) {
            setMessage(err.response?.data?.message || "Failed to remove item");
        }
    };


    const handleApplyCoupon = async () => {
        if (!couponCode) return;
        try {
            const res = await couponService.applyCoupon(couponCode, totalAmount);
            setDiscountAmount(res.data.discountAmount);
            setFinalTotal(res.data.newTotal);
            setAppliedCoupon(couponCode);
            setCouponMessage(res.data.message);
        } catch (err) {
            setCouponMessage(err.response?.data?.error || "Error applying coupon");
            setAppliedCoupon(null);
            setDiscountAmount(0);
            setFinalTotal(totalAmount);
        }
    };

    const handleCheckout = async (e) => {
        e.preventDefault();
        if (cartItems.length === 0) {
            setMessage("Your cart is empty");
            return;
        }
        if (!shippingAddress.trim()) {
            setMessage("Please enter shipping address");
            return;
        }

        try {
            const orderData = {
                items: cartItems.map(item => ({
                    productId: item.productId,
                    quantity: item.quantity
                })),
                shippingAddress: shippingAddress,
                couponCode: appliedCoupon
            };

            await orderService.createOrder(orderData);
            setMessage("Order placed successfully!");
            setShippingAddress("");
            setTimeout(() => navigate("/orders"), 1500);
        } catch (err) {
            setMessage(err.response?.data?.error || err.response?.data?.message || "Failed to place order");
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
            <div className="container py-5 text-center">
                <div className="spinner-border" role="status"></div>
            </div>
        );
    }

    return (
        <div className="container py-5">
            <div className="row">
                <div className="col-md-8">
                    <h2 className="mb-4">Shopping Cart</h2>
                    {message && <div className="alert alert-info">{message}</div>}

                    {cartItems.length === 0 ? (
                        <div className="admin-card p-5 text-center shadow-sm">
                            <h4 className="text-secondary mb-3">Your cart is empty</h4>
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
                                            <th>Product</th>
                                            <th>Price</th>
                                            <th>Quantity</th>
                                            <th>Total</th>
                                            <th>Action</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {cartItems.map((item) => (
                                            <tr key={item.id}>
                                                <td>
                                                    <div>
                                                        <h6 className="mb-0">{item.productName}</h6>
                                                        <small className="text-muted">ID: {item.productId}</small>
                                                    </div>
                                                </td>
                                                <td>{formatCurrency(item.price)}</td>
                                                <td>
                                                    <div className="d-flex align-items-center">
                                                        <button
                                                            className="btn btn-sm btn-outline-secondary"
                                                            onClick={() => handleQuantityChange(item.id, item.quantity - 1)}
                                                            disabled={updatingItem === item.id}
                                                        >
                                                            -
                                                        </button>
                                                        <span className="mx-2">{item.quantity}</span>
                                                        <button
                                                            className="btn btn-sm btn-outline-secondary"
                                                            onClick={() => handleQuantityChange(item.id, item.quantity + 1)}
                                                            disabled={updatingItem === item.id}
                                                        >
                                                            +
                                                        </button>
                                                        {updatingItem === item.id && (
                                                            <span className="spinner-border spinner-border-sm ms-2"></span>
                                                        )}
                                                    </div>
                                                </td>
                                                <td>{formatCurrency(item.totalPrice)}</td>
                                                <td>
                                                    <button
                                                        className="btn btn-sm btn-danger"
                                                        onClick={() => handleRemoveItem(item.id)}
                                                    >
                                                        Remove
                                                    </button>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                            <div className="mt-3">
                                <h5>Cart Total: {formatCurrency(totalAmount)}</h5>
                            </div>
                        </div>
                    )}
                </div>

                <div className="col-md-4">
                    <div className="admin-card p-4 shadow-sm">
                        <h4 className="mb-4">Checkout</h4>

                        <form onSubmit={handleCheckout}>
                            <div className="mb-3">
                                <label htmlFor="shippingAddress" className="form-label">
                                    Shipping Address
                                </label>
                                <textarea
                                    id="shippingAddress"
                                    className="form-control"
                                    rows="3"
                                    value={shippingAddress}
                                    onChange={(e) => setShippingAddress(e.target.value)}
                                    placeholder="Enter your complete shipping address"
                                    required
                                />
                            </div>

                            <div className="mb-4">
                                <label className="form-label">Apply Coupon</label>
                                <div className="input-group">
                                    <input
                                        type="text"
                                        className="form-control"
                                        placeholder="Coupon code"
                                        value={couponCode}
                                        onChange={e => setCouponCode(e.target.value.toUpperCase())}
                                        disabled={appliedCoupon != null}
                                    />
                                    {appliedCoupon ? (
                                        <button className="btn btn-outline-danger" type="button" onClick={() => {
                                            setAppliedCoupon(null);
                                            setDiscountAmount(0);
                                            setFinalTotal(totalAmount);
                                            setCouponCode("");
                                            setCouponMessage("");
                                        }}>Remove</button>
                                    ) : (
                                        <button className="btn btn-outline-secondary" type="button" onClick={handleApplyCoupon}>Apply</button>
                                    )}
                                </div>
                                {couponMessage && (
                                    <div className={`mt-2 small ${appliedCoupon ? 'text-success' : 'text-danger'}`}>
                                        {couponMessage}
                                    </div>
                                )}
                            </div>

                            <div className="mb-3 border-top pt-3">
                                <div className="d-flex justify-content-between mb-2">
                                    <span>Subtotal:</span>
                                    <span>{formatCurrency(totalAmount)}</span>
                                </div>
                                {appliedCoupon && (
                                    <div className="d-flex justify-content-between mb-2 text-success">
                                        <span>Discount:</span>
                                        <span>-{formatCurrency(discountAmount)}</span>
                                    </div>
                                )}
                                <div className="d-flex justify-content-between fw-bold h5">
                                    <span>Total:</span>
                                    <span>{formatCurrency(finalTotal)}</span>
                                </div>
                            </div>
                            <button
                                type="submit"
                                className="btn-primary-tech w-100"
                                disabled={cartItems.length === 0}
                            >
                                Place Order
                            </button>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Cart;
