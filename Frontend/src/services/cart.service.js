import axios from "axios";
import authHeader from "./auth-header";

const API_BASE = import.meta.env.VITE_API_URL || "http://localhost:8080/api/auth/";
const API_URL = API_BASE.replace("/auth/", "/cart");

const addToCart = (productId, quantity) => {
    return axios.post(API_URL + "/add", { productId, quantity }, { headers: authHeader() });
};

const updateCartItem = (itemId, quantity) => {
    return axios.put(API_URL + "/update", { itemId, quantity }, { headers: authHeader() });
};

const removeFromCart = (itemId) => {
    return axios.delete(API_URL + "/remove/" + itemId, { headers: authHeader() });
};

const getMyCart = () => {
    return axios.get(API_URL + "/my", { headers: authHeader() });
};

const getAllCarts = () => {
    return axios.get(API_URL + "/all", { headers: authHeader() });
};

const cartService = {
    addToCart,
    updateCartItem,
    removeFromCart,
    getMyCart,
    getAllCarts,
};

export default cartService;
