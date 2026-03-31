import axios from "axios";

const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080/api/auth/";
const PUBLIC_PRODUCT_API_URL = API_URL.replace("/auth/", "/products/public");

const getAll = () => {
    return axios.get(PUBLIC_PRODUCT_API_URL);
};

const publicProductService = {
    getAll,
};

export default publicProductService;
