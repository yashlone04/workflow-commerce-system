import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import AuthService from "../../services/auth.service";

const Login = () => {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState("");

    const navigate = useNavigate();

    const handleLogin = (e) => {
        e.preventDefault();
        setMessage("");
        setLoading(true);

        AuthService.login(username, password).then(
            () => {
                navigate("/home");
                window.location.reload();
            },
            (error) => {
                const resMessage =
                    (error.response &&
                        error.response.data &&
                        error.response.data.message) ||
                    error.message ||
                    error.toString();

                setLoading(false);
                setMessage(resMessage);
            }
        );
    };

    return (
        <div className="container py-5 mt-5 animate-fade-in">
            <div className="row justify-content-center">
                <div className="col-md-5 col-lg-4">
                    <div className="admin-card p-4 p-md-5 shadow-lg border-0">
                        <div className="text-center mb-4">
                            <div className="bg-primary text-white rounded-3 mx-auto mb-3 d-flex align-items-center justify-content-center" style={{ width: '48px', height: '48px', fontSize: '24px' }}>
                                W
                            </div>
                            <h2 className="fw-bold h4 mb-1">Administrative Sign In</h2>
                            <p className="text-secondary small">Access the Workflow Console</p>
                        </div>

                        {message && (
                            <div className="alert alert-danger py-2 px-3 small border-0 mb-4 text-center" role="alert">
                                {message}
                            </div>
                        )}

                        <form onSubmit={handleLogin}>
                            <div className="mb-3">
                                <label className="form-label small fw-semibold text-secondary">Username</label>
                                <input
                                    type="text"
                                    className="form-input-tech"
                                    value={username}
                                    onChange={(e) => setUsername(e.target.value)}
                                    placeholder="Enter your username"
                                    required
                                />
                            </div>

                            <div className="mb-4">
                                <label className="form-label small fw-semibold text-secondary">Password</label>
                                <input
                                    type="password"
                                    className="form-input-tech"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    placeholder="••••••••"
                                    required
                                />
                            </div>

                            <button className="btn-primary-tech w-100 py-2 mb-4 d-flex align-items-center justify-content-center gap-2" disabled={loading}>
                                {loading && <span className="spinner-border spinner-border-sm"></span>}
                                {loading ? "Authenticating..." : "Sign In to Console"}
                            </button>

                            <p className="text-center text-secondary small mb-0">
                                New to the infrastructure? <Link to="/register" className="text-primary text-decoration-none fw-medium">Request access</Link>
                            </p>
                        </form>
                    </div>
                    <p className="text-center text-muted small mt-4">
                        Authorized Personnel Only • Secure JWT Protocol
                    </p>
                </div>
            </div>
        </div>
    );
};

export default Login;
