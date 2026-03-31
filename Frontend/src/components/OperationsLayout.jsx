import React, { useState, useEffect } from "react";
import { Link, useLocation, useNavigate, Outlet } from "react-router-dom";
import AuthService from "../services/auth.service";

const OperationsLayout = () => {
    const [currentUser, setCurrentUser] = useState(undefined);
    const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
    const location = useLocation();
    const navigate = useNavigate();

    useEffect(() => {
        const user = AuthService.getCurrentUser();
        if (user) {
            setCurrentUser(user);
        }
    }, [location]);

    const logOut = () => {
        AuthService.logout();
        setCurrentUser(undefined);
        navigate("/login");
        window.location.reload();
    };

    const isAdmin = currentUser?.roles?.includes("ROLE_ADMIN");
    const isActive = (path) => location.pathname === path || location.pathname.startsWith(path + '/');

    const menuSections = [
        {
            title: "Overview",
            items: [
                { path: "/ops/dashboard", icon: "bi-grid-1x2", label: "Dashboard" },
                { path: "/ops/workflows", icon: "bi-diagram-3", label: "Workflows" },
            ]
        },
        {
            title: "Order Operations",
            items: [
                { path: "/ops/orders", icon: "bi-receipt", label: "Orders" },
                { path: "/ops/payments", icon: "bi-credit-card", label: "Payments" },
                { path: "/ops/shipping", icon: "bi-truck", label: "Shipping" },
            ]
        },
        {
            title: "Catalog Management",
            items: [
                { path: "/ops/products", icon: "bi-box-seam", label: "Products" },
                { path: "/ops/categories", icon: "bi-tags", label: "Categories" },
            ]
        },
        {
            title: "Customer Operations",
            items: [
                { path: "/ops/customers", icon: "bi-people", label: "Customers" },
                { path: "/ops/carts", icon: "bi-cart3", label: "Carts" },
                { path: "/ops/reviews", icon: "bi-star", label: "Reviews" },
                { path: "/ops/coupons", icon: "bi-ticket-perforated", label: "Coupons" },
            ]
        },
        {
            title: "Activity",
            items: [
                { path: "/ops/logs", icon: "bi-clock-history", label: "Audit Logs" },
            ]
        }
    ];

    if (!currentUser) {
        return (
            <div className="ops-auth-required d-flex align-items-center justify-content-center" style={{ minHeight: '100vh', background: '#f8f9fa' }}>
                <div className="text-center p-5 bg-white rounded-3 shadow-sm">
                    <div className="mb-4">
                        <i className="bi bi-shield-lock fs-1 text-secondary"></i>
                    </div>
                    <h4 className="fw-semibold mb-3">Authentication Required</h4>
                    <p className="text-secondary mb-4">Sign in to access Operations Console</p>
                    <Link to="/login" className="btn btn-primary px-4">Sign In</Link>
                </div>
            </div>
        );
    }

    if (!isAdmin) {
        return (
            <div className="ops-access-denied d-flex align-items-center justify-content-center" style={{ minHeight: '100vh', background: '#f8f9fa' }}>
                <div className="text-center p-5 bg-white rounded-3 shadow-sm">
                    <div className="mb-4">
                        <i className="bi bi-x-octagon fs-1 text-danger"></i>
                    </div>
                    <h4 className="fw-semibold mb-3">Access Denied</h4>
                    <p className="text-secondary mb-4">You need administrator permissions to access this console.</p>
                    <Link to="/home" className="btn btn-outline-secondary px-4">Return Home</Link>
                </div>
            </div>
        );
    }

    return (
        <div className="ops-layout d-flex" style={{ minHeight: '100vh' }}>
            {/* Sidebar */}
            <aside
                className={`ops-sidebar bg-dark text-white d-flex flex-column ${sidebarCollapsed ? 'collapsed' : ''}`}
                style={{
                    width: sidebarCollapsed ? '72px' : '260px',
                    transition: 'width 0.2s ease',
                    position: 'fixed',
                    top: 0,
                    left: 0,
                    bottom: 0,
                    zIndex: 1000
                }}
            >
                {/* Logo */}
                <div className="ops-sidebar-header px-3 py-3 border-bottom border-secondary d-flex align-items-center justify-content-between">
                    <Link to="/ops/dashboard" className="text-decoration-none d-flex align-items-center">
                        <div className="bg-primary rounded-2 d-flex align-items-center justify-content-center" style={{ width: '36px', height: '36px' }}>
                            <i className="bi bi-diagram-3 text-white"></i>
                        </div>
                        {!sidebarCollapsed && (
                            <span className="ms-2 fw-semibold text-white" style={{ fontSize: '15px' }}>Workflow Ops</span>
                        )}
                    </Link>
                    <button
                        className="btn btn-link text-secondary p-0 border-0"
                        onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
                        title={sidebarCollapsed ? "Expand" : "Collapse"}
                    >
                        <i className={`bi ${sidebarCollapsed ? 'bi-chevron-right' : 'bi-chevron-left'}`}></i>
                    </button>
                </div>

                {/* Navigation */}
                <nav className="ops-sidebar-nav flex-grow-1 overflow-auto py-2">
                    {menuSections.map((section, idx) => (
                        <div key={idx} className="mb-2">
                            {!sidebarCollapsed && (
                                <div className="px-3 py-2 text-uppercase text-secondary" style={{ fontSize: '11px', letterSpacing: '0.5px', fontWeight: 600 }}>
                                    {section.title}
                                </div>
                            )}
                            {section.items.map((item) => (
                                <Link
                                    key={item.path}
                                    to={item.path}
                                    className={`ops-nav-item d-flex align-items-center px-3 py-2 text-decoration-none mx-2 rounded-2 ${isActive(item.path)
                                            ? 'bg-primary text-white'
                                            : 'text-secondary-emphasis'
                                        }`}
                                    style={{
                                        fontSize: '14px',
                                        transition: 'all 0.15s ease'
                                    }}
                                    title={sidebarCollapsed ? item.label : ''}
                                >
                                    <i className={`bi ${item.icon} ${sidebarCollapsed ? '' : 'me-3'}`} style={{ fontSize: '16px' }}></i>
                                    {!sidebarCollapsed && <span>{item.label}</span>}
                                </Link>
                            ))}
                        </div>
                    ))}
                </nav>

                {/* User Section */}
                <div className="ops-sidebar-footer border-top border-secondary p-3">
                    <div className="d-flex align-items-center">
                        <div className="bg-secondary rounded-circle d-flex align-items-center justify-content-center" style={{ width: '36px', height: '36px' }}>
                            <i className="bi bi-person text-white"></i>
                        </div>
                        {!sidebarCollapsed && (
                            <div className="ms-2 flex-grow-1">
                                <div className="text-white small fw-medium">{currentUser.username}</div>
                                <div className="text-secondary" style={{ fontSize: '11px' }}>Administrator</div>
                            </div>
                        )}
                        {!sidebarCollapsed && (
                            <button
                                className="btn btn-link text-secondary p-0"
                                onClick={logOut}
                                title="Sign Out"
                            >
                                <i className="bi bi-box-arrow-right"></i>
                            </button>
                        )}
                    </div>
                </div>
            </aside>

            {/* Main Content */}
            <main
                className="ops-main flex-grow-1"
                style={{
                    marginLeft: sidebarCollapsed ? '72px' : '260px',
                    transition: 'margin-left 0.2s ease',
                    background: '#f4f6f8',
                    minHeight: '100vh'
                }}
            >
                {/* Top Bar */}
                <header className="ops-topbar bg-white border-bottom px-4 py-3 d-flex align-items-center justify-content-between sticky-top">
                    <div className="d-flex align-items-center">
                        <nav aria-label="breadcrumb">
                            <ol className="breadcrumb mb-0 small">
                                <li className="breadcrumb-item"><Link to="/ops/dashboard" className="text-decoration-none">Ops</Link></li>
                                <li className="breadcrumb-item active" aria-current="page">
                                    {location.pathname.split('/').pop().replace(/-/g, ' ').replace(/\b\w/g, l => l.toUpperCase())}
                                </li>
                            </ol>
                        </nav>
                    </div>
                    <div className="d-flex align-items-center gap-3">
                        <Link to="/home" className="btn btn-sm btn-outline-secondary">
                            <i className="bi bi-house me-1"></i>
                            Customer View
                        </Link>
                        <div className="dropdown">
                            <button className="btn btn-sm btn-light dropdown-toggle" data-bs-toggle="dropdown">
                                <i className="bi bi-gear me-1"></i>
                            </button>
                            <ul className="dropdown-menu dropdown-menu-end">
                                <li><Link to="/profile" className="dropdown-item">Profile</Link></li>
                                <li><hr className="dropdown-divider" /></li>
                                <li><button className="dropdown-item text-danger" onClick={logOut}>Sign Out</button></li>
                            </ul>
                        </div>
                    </div>
                </header>

                {/* Page Content */}
                <div className="ops-content p-4">
                    <Outlet />
                </div>
            </main>
        </div>
    );
};

export default OperationsLayout;
