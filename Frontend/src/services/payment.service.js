import axios from "axios";
import authHeader from "./auth-header";

const API_BASE = import.meta.env.VITE_API_URL || "http://localhost:8080/api/auth/";
const API_URL = API_BASE.replace("/auth/", "/payments");

const processPayment = (orderId, paymentMethod) => {
    return axios.post(API_URL + "/" + orderId, { paymentMethod }, { headers: authHeader() });
};

const getAllPayments = (params = {}) => {
    return axios.get(API_URL, { headers: authHeader(), params });
};

const refundPayment = (paymentId) => {
    return axios.put(API_URL + "/" + paymentId + "/refund", {}, { headers: authHeader() });
};

const paymentService = {
    processPayment,
    getAllPayments,
    refundPayment,
};

export default paymentService;
