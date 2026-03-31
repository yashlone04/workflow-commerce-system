import React, { Component } from 'react';

class ErrorBoundary extends Component {
    constructor(props) {
        super(props);
        this.state = { hasError: false, error: null, errorInfo: null };
    }

    static getDerivedStateFromError(error) {
        return { hasError: true };
    }

    componentDidCatch(error, errorInfo) {
        this.setState({
            error: error,
            errorInfo: errorInfo
        });
        // Log error to console in development
        console.error('ErrorBoundary caught an error:', error, errorInfo);
    }

    handleReload = () => {
        window.location.reload();
    };

    handleGoHome = () => {
        window.location.href = '/';
    };

    render() {
        if (this.state.hasError) {
            return (
                <div className="container py-5 text-center">
                    <div className="card shadow-sm p-5 d-inline-block" style={{ maxWidth: '500px' }}>
                        <div className="mb-4">
                            <i className="bi bi-exclamation-triangle text-danger" style={{ fontSize: '4rem' }}></i>
                        </div>
                        <h2 className="text-danger fw-bold mb-3">Something went wrong</h2>
                        <p className="text-secondary mb-4">
                            We encountered an unexpected error. Please try refreshing the page or return to the home page.
                        </p>
                        {process.env.NODE_ENV === 'development' && this.state.error && (
                            <details className="text-start mb-4">
                                <summary className="text-muted cursor-pointer">Error Details</summary>
                                <pre className="bg-light p-3 rounded mt-2 small overflow-auto" style={{ maxHeight: '200px' }}>
                                    {this.state.error.toString()}
                                    {this.state.errorInfo?.componentStack}
                                </pre>
                            </details>
                        )}
                        <div className="d-flex gap-3 justify-content-center">
                            <button
                                onClick={this.handleReload}
                                className="btn btn-primary"
                            >
                                <i className="bi bi-arrow-clockwise me-2"></i>
                                Refresh Page
                            </button>
                            <button
                                onClick={this.handleGoHome}
                                className="btn btn-outline-secondary"
                            >
                                <i className="bi bi-house me-2"></i>
                                Go Home
                            </button>
                        </div>
                    </div>
                </div>
            );
        }

        return this.props.children;
    }
}

export default ErrorBoundary;
