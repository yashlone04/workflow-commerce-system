import axios from "axios";
import authHeader from "./auth-header";

const API_BASE = import.meta.env.VITE_API_URL || "http://localhost:8080/api/auth/";
const ORDER_API_URL = API_BASE.replace("/auth/", "/orders");

const createOrder = (orderData) => {
    return axios.post(ORDER_API_URL, orderData, { headers: authHeader() });
};

const getMyOrders = () => {
    return axios.get(ORDER_API_URL + "/my", { headers: authHeader() });
};

const cancelOrder = (orderId) => {
    return axios.put(ORDER_API_URL + "/" + orderId + "/cancel/user", {}, { headers: authHeader() });
};

const orderService = {
    createOrder,
    getMyOrders,
    cancelOrder,
};

export default orderService;
