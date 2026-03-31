import React, { useState } from "react";
import { Link } from "react-router-dom";
import AuthService from "../../services/auth.service";

const Register = () => {
    const [username, setUsername] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [successful, setSuccessful] = useState(false);
    const [message, setMessage] = useState("");
    const [loading, setLoading] = useState(false);

    const handleRegister = (e) => {
        e.preventDefault();
        setMessage("");
        setSuccessful(false);
        setLoading(true);

        AuthService.register(username, email, password, ["user"]).then(
            (response) => {
                setMessage(response.data.message);
                setSuccessful(true);
                setLoading(false);
            },
            (error) => {
                const resMessage =
                    (error.response &&
                        error.response.data &&
                        error.response.data.message) ||
                    error.message ||
                    error.toString();

                setMessage(resMessage);
                setSuccessful(false);
                setLoading(false);
            }
        );
    };

    return (
        <div className="container py-5 mt-4 animate-fade-in">
            <div className="row justify-content-center">
                <div className="col-md-6 col-lg-5">
                    <div className="admin-card p-4 p-md-5 shadow-lg border-0">
                        <div className="text-center mb-4">
                            <div className="bg-primary text-white rounded-3 mx-auto mb-3 d-flex align-items-center justify-content-center" style={{ width: '48px', height: '48px', fontSize: '24px' }}>
                                W
                            </div>
                            <h2 className="fw-bold h4 mb-1">{successful ? "Registration Complete" : "Request System Access"}</h2>
                            <p className="text-secondary small">Establish your credentials in the workflow infrastructure</p>
                        </div>

                        {message && (
                            <div className={`alert ${successful ? 'alert-success' : 'alert-danger'} border-0 py-2 px-3 small text-center mb-4`} role="alert">
                                {message}
                                {successful && (
                                    <div className="mt-3">
                                        <Link to="/login" className="btn-primary-tech py-2 px-4 shadow-sm text-decoration-none d-inline-block">Proceed to Login</Link>
                                    </div>
                                )}
                            </div>
                        )}

                        {!successful && (
                            <form onSubmit={handleRegister}>
                                <div className="mb-3">
                                    <label className="form-label small fw-semibold text-secondary">Username</label>
                                    <input
                                        type="text"
                                        className="form-input-tech"
                                        value={username}
                                        onChange={(e) => setUsername(e.target.value)}
                                        minLength={3}
                                        placeholder="Pick a unique identifier"
                                        required
                                    />
                                </div>

                                <div className="mb-3">
                                    <label className="form-label small fw-semibold text-secondary">Email Address</label>
                                    <input
                                        type="email"
                                        className="form-input-tech"
                                        value={email}
                                        onChange={(e) => setEmail(e.target.value)}
                                        placeholder="user@organization.com"
                                        required
                                    />
                                </div>

                                <div className="mb-3">
                                    <label className="form-label small fw-semibold text-secondary">Password</label>
                                    <input
                                        type="password"
                                        className="form-input-tech"
                                        value={password}
                                        onChange={(e) => setPassword(e.target.value)}
                                        minLength={6}
                                        placeholder="Min. 6 characters"
                                        required
                                    />
                                </div>

                                <button className="btn-primary-tech w-100 py-2 mb-4 d-flex align-items-center justify-content-center gap-2" disabled={loading}>
                                    {loading && <span className="spinner-border spinner-border-sm"></span>}
                                    {loading ? "Registering..." : "Provision Account"}
                                </button>

                                <p className="text-center text-secondary small mb-0">
                                    Already have an account? <Link to="/login" className="text-primary text-decoration-none fw-medium">Sign in</Link>
                                </p>
                            </form>
                        )}
                    </div>
                    <p className="text-center text-muted small mt-4">
                        Secure Registration • JWT Authentication
                    </p>
                </div>
            </div>
        </div>
    );
};

export default Register;
