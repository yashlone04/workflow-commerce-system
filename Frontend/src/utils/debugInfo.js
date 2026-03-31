// Debug information for admin dashboard infinite loop issues

export const debugAdminDashboard = () => {
    console.log('=== Admin Dashboard Debug Info ===');
    
    // Check authentication
    const user = JSON.parse(localStorage.getItem('user'));
    console.log('User from localStorage:', user ? {
        username: user.username,
        roles: user.roles,
        hasToken: !!user.token,
        tokenPreview: user.token ? `${user.token.substring(0, 20)}...` : 'No token'
    } : 'No user found');
    
    // Check token expiration
    if (user && user.token) {
        try {
            const tokenPayload = JSON.parse(atob(user.token.split('.')[1]));
            const currentTime = Date.now() / 1000;
            const isExpired = tokenPayload.exp < currentTime;
            const expiryTime = new Date(tokenPayload.exp * 1000);
            console.log('Token info:', {
                isExpired,
                expiryTime: expiryTime.toISOString(),
                currentTime: new Date(currentTime * 1000).toISOString(),
                timeUntilExpiry: Math.floor(tokenPayload.exp - currentTime) + ' seconds'
            });
        } catch (error) {
            console.error('Token parsing error:', error);
        }
    }
    
    // Check for potential infinite loops
    console.log('Potential infinite loop causes:');
    console.log('1. Check browser console for repeated API calls');
    console.log('2. Look for "fetchOrders called while loading, skipping" messages');
    console.log('3. Check if useEffect dependencies are changing frequently');
    console.log('4. Verify authentication state is stable');
    
    console.log('=== End Debug Info ===');
};

export const clearAllData = () => {
    localStorage.removeItem('user');
    localStorage.removeItem('pendingCheckout');
    console.log('All local storage data cleared');
};
