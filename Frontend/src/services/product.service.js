import axios from "axios";
import authHeader from "./auth-header";

const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080/api/auth/";
const PRODUCT_API_URL = API_URL.replace("/auth/", "/products");

const getAll = () => {
    return axios.get(PRODUCT_API_URL, { headers: authHeader() });
};

const create = (data) => {
    return axios.post(PRODUCT_API_URL, data, { headers: authHeader() });
};

const update = (id, data) => {
    return axios.put(`${PRODUCT_API_URL}/${id}`, data, { headers: authHeader() });
};

const deactivate = (id) => {
    return axios.delete(`${PRODUCT_API_URL}/${id}`, { headers: authHeader() });
};

const productService = {
    getAll,
    create,
    update,
    deactivate,
};

export default productService;
