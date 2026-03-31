import axios from "axios";
import authHeader from "./auth-header";

const API_URL = "http://localhost:8080/api/reviews/";

const addReview = (productId, rating, reviewText) => {
    return axios.post(API_URL + `add/${productId}`, { rating, reviewText }, { headers: authHeader() });
};

const updateReview = (reviewId, rating, reviewText) => {
    return axios.put(API_URL + `update/${reviewId}`, { rating, reviewText }, { headers: authHeader() });
};

const deleteReview = (reviewId) => {
    return axios.delete(API_URL + `delete/${reviewId}`, { headers: authHeader() });
};

const approveReview = (reviewId) => {
    return axios.put(API_URL + `approve/${reviewId}`, {}, { headers: authHeader() });
};

const rejectReview = (reviewId) => {
    return axios.put(API_URL + `reject/${reviewId}`, {}, { headers: authHeader() });
};

const getAllReviews = () => {
    return axios.get(API_URL + "all", { headers: authHeader() });
};

const getProductReviews = (productId) => {
    return axios.get(API_URL + `product/${productId}`);
};

const getMyReviews = () => {
    return axios.get(API_URL + "my", { headers: authHeader() });
};

export default {
    addReview,
    updateReview,
    deleteReview,
    approveReview,
    rejectReview,
    getAllReviews,
    getProductReviews,
    getMyReviews
};
