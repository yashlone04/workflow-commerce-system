import React, { useState, useEffect } from "react";
import { Routes, Route, Link } from "react-router-dom";
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap-icons/font/bootstrap-icons.css";
import "./App.css";

import Navbar from "./components/Navbar";
import ErrorBoundary from "./components/ErrorBoundary";
import Login from "./pages/Login/Login";
import Register from "./pages/Register/Register";
import CategoryDashboard from "./pages/Admin/CategoryDashboard";
import AuthService from "./services/auth.service";
import ProductDashboard from "./pages/Admin/ProductDashboard";
import ProductCatalog from "./pages/Customer/ProductCatalog";
import Cart from "./pages/Customer/Cart";
import MyOrders from "./pages/Customer/MyOrders";
import OrderDashboard from "./pages/Admin/OrderDashboard";
import CustomerDashboard from "./pages/Admin/CustomerDashboard";
import PaymentDashboard from "./pages/Admin/PaymentDashboard";
import CartDashboard from "./pages/Admin/CartDashboard";
import Wishlist from "./pages/Customer/Wishlist";
import Profile from "./pages/Customer/Profile";
import ShippingDashboard from "./pages/Admin/ShippingDashboard";
import ReviewDashboard from "./pages/Admin/ReviewDashboard";
import CouponDashboard from "./pages/Admin/CouponDashboard";
import { CartProvider } from "./contexts/CartContext";
import { AuthProvider } from "./contexts/AuthContext";

// Operations Dashboard imports
import OperationsLayout from "./components/OperationsLayout";
import OperationsDashboard from "./pages/Operations/OperationsDashboard";
import WorkflowsPage from "./pages/Operations/WorkflowsPage";
import WorkflowDetails from "./pages/Operations/WorkflowDetails";
import AuditLogsPage from "./pages/Operations/AuditLogsPage";
import OpsOrdersPage from "./pages/Operations/OpsOrdersPage";

// Admin Route Guard Component
const AdminRoute = ({ children }) => {
  const [isAdmin, setIsAdmin] = useState(null);

  useEffect(() => {
    const user = AuthService.getCurrentUser();
    setIsAdmin(user?.roles?.includes("ROLE_ADMIN") || false);
  }, []);

  if (isAdmin === null) {
    return <div className="container py-5 text-center">Loading...</div>;
  }

  if (!isAdmin) {
    return (
      <div className="container py-5 text-center">
        <div className="admin-card p-5 d-inline-block shadow-sm">
          <h2 className="text-danger fw-bold mb-3">Access Denied</h2>
          <p className="text-secondary mb-4">You do not have the required administrative permissions to access this console.</p>
          <Link to="/home" className="btn-primary-tech px-4 py-2 text-decoration-none">Return Home</Link>
        </div>
      </div>
    );
  }

  return children;
};

const Home = () => {
  const [isAdmin, setIsAdmin] = useState(false);

  useEffect(() => {
    const user = AuthService.getCurrentUser();
    setIsAdmin(user?.roles?.includes("ROLE_ADMIN") || false);
  }, []);

  return (
    <div className="container py-5 mt-5">
      <div className="row align-items-center">
        <div className="col-lg-6">
          <div className="badge bg-primary-light text-primary px-3 py-2 rounded-pill mb-4 small fw-bold">
            WORKFLOW-DRIVEN COMMERCE PLATFORM
          </div>
          <h1 className="display-4 fw-bold mb-4">
            Workflow Commerce<br />Operations Platform.
          </h1>
          <p className="lead text-secondary mb-5">
            A scalable commerce platform with workflow orchestration,
            state-machine driven order lifecycle management, and enterprise-grade operations tooling.
          </p>
          <div className="d-flex gap-3 flex-wrap">
            {isAdmin && (
              <Link to="/ops/dashboard" className="btn btn-primary px-4 py-2 text-decoration-none shadow-sm">
                <i className="bi bi-diagram-3 me-2"></i>
                Operations Console
              </Link>
            )}
            <Link to="/login" className="btn-primary-tech px-4 py-2 text-decoration-none shadow-sm">
              Launch Console
            </Link>
            <Link to="/register" className="btn-secondary-tech px-4 py-2 text-decoration-none">
              Register Account
            </Link>
            <Link to="/products" className="btn-outline-tech px-4 py-2 text-decoration-none">
              Browse Products
            </Link>
          </div>
        </div>
        <div className="col-lg-6 d-none d-lg-block">
          <div className="admin-card p-5 bg-light border-0 shadow-sm text-center">
            <h4 className="text-secondary mb-3">Workflow Engine v2.0</h4>
            <p className="text-muted small">Systems Operational • State Machine Active</p>
            <div className="d-flex justify-content-center gap-2 mt-4">
              <div className="bg-white p-3 rounded shadow-sm" style={{ width: '80px' }}>
                <div className="small text-muted mb-1">State</div>
                <div className="fw-bold text-success small">ACTIVE</div>
              </div>
              <div className="bg-white p-3 rounded shadow-sm" style={{ width: '80px' }}>
                <div className="small text-muted mb-1">Queue</div>
                <div className="fw-bold text-primary small">0.0ms</div>
              </div>
              <div className="bg-white p-3 rounded shadow-sm" style={{ width: '80px' }}>
                <div className="small text-muted mb-1">Logs</div>
                <div className="fw-bold text-secondary small">READY</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

const Footer = () => (
  <footer className="container py-4 mt-5 border-top text-center">
    <p className="text-secondary small mb-0">© 2026 Workflow Commerce. Built with React, Spring Boot & MySQL.</p>
  </footer>
);

function App() {
  return (
    <ErrorBoundary>
      <AuthProvider>
        <CartProvider>
          <Routes>
            {/* Operations Console - Separate layout with sidebar */}
            <Route path="/ops" element={<OperationsLayout />}>
              <Route index element={<OperationsDashboard />} />
              <Route path="dashboard" element={<OperationsDashboard />} />
              <Route path="workflows" element={<WorkflowsPage />} />
              <Route path="workflows/:instanceId" element={<WorkflowDetails />} />
              <Route path="orders" element={<OpsOrdersPage />} />
              <Route path="payments" element={<PaymentDashboard />} />
              <Route path="shipping" element={<ShippingDashboard />} />
              <Route path="products" element={<ProductDashboard />} />
              <Route path="categories" element={<CategoryDashboard />} />
              <Route path="customers" element={<CustomerDashboard />} />
              <Route path="carts" element={<CartDashboard />} />
              <Route path="reviews" element={<ReviewDashboard />} />
              <Route path="coupons" element={<CouponDashboard />} />
              <Route path="logs" element={<AuditLogsPage />} />
            </Route>

            {/* Customer-facing routes with standard Navbar/Footer */}
            <Route path="/*" element={
              <div>
                <Navbar />
                <div className="container">
                  <Routes>
                    <Route path="/" element={<Home />} />
                    <Route path="/home" element={<Home />} />
                    <Route path="/login" element={<Login />} />
                    <Route path="/register" element={<Register />} />
                    <Route path="/products" element={<ProductCatalog />} />
                    <Route path="/cart" element={<Cart />} />
                    <Route path="/wishlist" element={<Wishlist />} />
                    <Route path="/orders" element={<MyOrders />} />
                    <Route path="/profile" element={<Profile />} />
                    <Route
                      path="/admin/categories"
                      element={
                        <AdminRoute>
                          <CategoryDashboard />
                        </AdminRoute>
                      }
                    />
                    <Route
                      path="/admin/products"
                      element={
                        <AdminRoute>
                          <ProductDashboard />
                        </AdminRoute>
                      }
                    />
                    <Route
                      path="/admin/orders"
                      element={
                        <AdminRoute>
                          <OrderDashboard />
                        </AdminRoute>
                      }
                    />
                    <Route
                      path="/admin/customers"
                      element={
                        <AdminRoute>
                          <CustomerDashboard />
                        </AdminRoute>
                      }
                    />
                    <Route
                      path="/admin/payments"
                      element={
                        <AdminRoute>
                          <PaymentDashboard />
                        </AdminRoute>
                      }
                    />
                    <Route
                      path="/admin/carts"
                      element={
                        <AdminRoute>
                          <CartDashboard />
                        </AdminRoute>
                      }
                    />
                    <Route
                      path="/admin/shipping"
                      element={
                        <AdminRoute>
                          <ShippingDashboard />
                        </AdminRoute>
                      }
                    />
                    <Route
                      path="/admin/reviews"
                      element={
                        <AdminRoute>
                          <ReviewDashboard />
                        </AdminRoute>
                      }
                    />
                    <Route
                      path="/admin/coupons"
                      element={
                        <AdminRoute>
                          <CouponDashboard />
                        </AdminRoute>
                      }
                    />
                  </Routes>
                </div>
                <Footer />
              </div>
            } />
          </Routes>
        </CartProvider>
      </AuthProvider>
    </ErrorBoundary>
  );
}

export default App;
