import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import AuthService from '../services/auth.service';

const AuthContext = createContext();

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [isAdmin, setIsAdmin] = useState(false);
    const [isTokenExpired, setIsTokenExpired] = useState(false);

    // Check token validity
    const checkTokenValidity = useCallback((userData) => {
        if (!userData?.token) return false;

        try {
            const tokenPayload = JSON.parse(atob(userData.token.split('.')[1]));
            const isExpired = tokenPayload.exp < Date.now() / 1000;
            setIsTokenExpired(isExpired);
            return !isExpired;
        } catch (error) {
            console.error('Error parsing token:', error);
            return false;
        }
    }, []);

    // Initialize auth state
    useEffect(() => {
        const initAuth = () => {
            const userData = AuthService.getCurrentUser();
            if (userData && checkTokenValidity(userData)) {
                setUser(userData);
                setIsAdmin(userData.roles?.includes('ROLE_ADMIN') || false);
            } else if (userData) {
                // Token expired, clear storage
                AuthService.logout();
                setUser(null);
                setIsAdmin(false);
            }
            setLoading(false);
        };

        initAuth();
    }, [checkTokenValidity]);

    // Login function
    const login = async (username, password) => {
        const response = await AuthService.login(username, password);
        setUser(response);
        setIsAdmin(response.roles?.includes('ROLE_ADMIN') || false);
        setIsTokenExpired(false);
        return response;
    };

    // Logout function
    const logout = useCallback(() => {
        AuthService.logout();
        setUser(null);
        setIsAdmin(false);
        setIsTokenExpired(false);
    }, []);

    // Register function
    const register = async (username, email, password) => {
        return AuthService.register(username, email, password);
    };

    // Check if user has specific role
    const hasRole = useCallback((role) => {
        return user?.roles?.includes(role) || false;
    }, [user]);

    // Refresh user data
    const refreshUser = useCallback(() => {
        const userData = AuthService.getCurrentUser();
        if (userData && checkTokenValidity(userData)) {
            setUser(userData);
            setIsAdmin(userData.roles?.includes('ROLE_ADMIN') || false);
        }
    }, [checkTokenValidity]);

    const value = {
        user,
        loading,
        isAdmin,
        isTokenExpired,
        isAuthenticated: !!user && !isTokenExpired,
        login,
        logout,
        register,
        hasRole,
        refreshUser,
    };

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
};

export default AuthContext;
