import axios from "axios";
import authHeader from "./auth-header";

const API_BASE = import.meta.env.VITE_API_URL || "http://localhost:8080/api/auth/";
const API_URL = API_BASE.replace("/auth/", "/shipping/");

const calculateShippingCost = (shippingMethod, destination) => {
    return axios.post(API_URL + "calculate-cost", { shippingMethod, destination }, { headers: authHeader() });
};

const createShipping = (orderId, shippingData) => {
    return axios.post(API_URL + "create/" + orderId, shippingData, { headers: authHeader() });
};

const updateShippingStatus = (shippingId, status) => {
    return axios.put(API_URL + "update-status/" + shippingId, { status }, { headers: authHeader() });
};

const getAllShippings = () => {
    return axios.get(API_URL + "all", { headers: authHeader() });
};

const getMyShipping = (orderId) => {
    return axios.get(API_URL + "my/" + orderId, { headers: authHeader() });
};

const shippingService = {
    calculateShippingCost,
    createShipping,
    updateShippingStatus,
    getAllShippings,
    getMyShipping
};

export default shippingService;
