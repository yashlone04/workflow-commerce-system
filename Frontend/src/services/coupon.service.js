import axios from "axios";
import authHeader from "./auth-header";

const API_URL = "http://localhost:8080/api/coupons/";

const createCoupon = (data) => {
    return axios.post(API_URL + "create", data, { headers: authHeader() });
};

const updateCoupon = (id, data) => {
    return axios.put(API_URL + `update/${id}`, data, { headers: authHeader() });
};

const deactivateCoupon = (id) => {
    return axios.put(API_URL + `deactivate/${id}`, {}, { headers: authHeader() });
};

const getAllCoupons = () => {
    return axios.get(API_URL + "all", { headers: authHeader() });
};

const applyCoupon = (couponCode, cartTotal) => {
    return axios.post(API_URL + "apply", { couponCode, cartTotal }, { headers: authHeader() });
};

export default {
    createCoupon,
    updateCoupon,
    deactivateCoupon,
    getAllCoupons,
    applyCoupon
};
