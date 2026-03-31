import axios from "axios";
import authHeader from "./auth-header";

const API_URL = "http://localhost:8080/api/wishlist";

const addToWishlist = (productId) => {
    return axios.post(API_URL + "/add/" + productId, {}, { headers: authHeader() });
};

const getMyWishlist = () => {
    return axios.get(API_URL + "/my", { headers: authHeader() });
};

const removeFromWishlist = (itemId) => {
    return axios.delete(API_URL + "/remove/" + itemId, { headers: authHeader() });
};

const moveToCart = (itemId) => {
    return axios.post(API_URL + "/move-to-cart/" + itemId, {}, { headers: authHeader() });
};

const checkProductInWishlist = (productId) => {
    return axios.get(API_URL + "/check/" + productId, { headers: authHeader() });
};

const wishlistService = {
    addToWishlist,
    getMyWishlist,
    removeFromWishlist,
    moveToCart,
    checkProductInWishlist,
};

export default wishlistService;
