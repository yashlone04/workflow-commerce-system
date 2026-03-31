// Global fetch tracker that persists across component remounts
export const globalFetchTracker = {
    orderDashboard: 0,
    productDashboard: 0,
    categoryDashboard: 0,
    myOrders: 0
};

export const shouldFetch = (component, throttleMs = 10000) => {
    const now = Date.now();
    const lastFetch = globalFetchTracker[component] || 0;
    const timeSince = now - lastFetch;
    
    if (timeSince < throttleMs) {
        console.log(`${component}: Global throttle - ${timeSince}ms since last fetch`);
        return false;
    }
    
    globalFetchTracker[component] = now;
    return true;
};

export const resetTracker = () => {
    Object.keys(globalFetchTracker).forEach(key => {
        globalFetchTracker[key] = 0;
    });
};
