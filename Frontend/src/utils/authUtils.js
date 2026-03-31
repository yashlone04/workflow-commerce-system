// Utility functions for authentication management

export const clearAuthData = () => {
    localStorage.removeItem('user');
    localStorage.removeItem('pendingCheckout');
    console.log('Authentication data cleared');
};

export const isTokenExpired = (token) => {
    if (!token) return true;
    
    try {
        const tokenPayload = JSON.parse(atob(token.split('.')[1]));
        const currentTime = Date.now() / 1000;
        return tokenPayload.exp < currentTime;
    } catch (error) {
        console.error('Invalid token format:', error);
        return true;
    }
};

export const validateAuth = () => {
    const user = JSON.parse(localStorage.getItem('user'));
    
    if (!user || !user.token) {
        return { valid: false, reason: 'No user or token found' };
    }
    
    if (isTokenExpired(user.token)) {
        return { valid: false, reason: 'Token expired' };
    }
    
    return { valid: true, user };
};

export const forceLogout = (navigate) => {
    clearAuthData();
    navigate('/login');
};
