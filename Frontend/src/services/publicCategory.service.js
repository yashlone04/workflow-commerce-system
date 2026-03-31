import axios from "axios";

const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080/api/auth/";
const PUBLIC_CATEGORY_API_URL = API_URL.replace("/auth/", "/categories/public");

const getAll = () => {
    return axios.get(PUBLIC_CATEGORY_API_URL);
};

const publicCategoryService = {
    getAll,
};

export default publicCategoryService;
