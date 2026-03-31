import axios from 'axios';
import AuthService from '../services/auth.service';

// Create axios instance with default config
const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api/auth/',
    timeout: 30000,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Request interceptor to add auth token
api.interceptors.request.use(
    (config) => {
        const user = AuthService.getCurrentUser();
        if (user?.token) {
            config.headers.Authorization = `Bearer ${user.token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Response interceptor for error handling
api.interceptors.response.use(
    (response) => response,
    (error) => {
        const originalRequest = error.config;

        // Handle 401 Unauthorized
        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;

            // Token expired or invalid - logout user
            AuthService.logout();

            // Redirect to login if not already there
            if (window.location.pathname !== '/login') {
                window.location.href = '/login';
            }

            return Promise.reject(new Error('Session expired. Please login again.'));
        }

        // Handle 403 Forbidden
        if (error.response?.status === 403) {
            console.error('Access denied:', error.response?.data?.message);
            return Promise.reject(new Error('You do not have permission to perform this action.'));
        }

        // Handle 500 Server Error
        if (error.response?.status >= 500) {
            console.error('Server error:', error.response?.data);
            return Promise.reject(new Error('Server error. Please try again later.'));
        }

        // Handle network errors
        if (!error.response) {
            console.error('Network error:', error.message);
            return Promise.reject(new Error('Network error. Please check your connection.'));
        }

        // Return original error for other cases
        return Promise.reject(error);
    }
);

export default api;
