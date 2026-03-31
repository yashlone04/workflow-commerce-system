import axios from "axios";
import authHeader from "./auth-header";

const API_BASE = import.meta.env.VITE_API_URL || "http://localhost:8080/api/auth/";
const API_URL = API_BASE.replace("/auth/", "/users");

const getAllUsers = (params = {}) => {
    return axios.get(API_URL, { headers: authHeader(), params });
};

const getUserById = (id) => {
    return axios.get(API_URL + "/" + id, { headers: authHeader() });
};

const getCurrentUser = () => {
    return axios.get(API_URL + "/me", { headers: authHeader() });
};

const createUser = (userData) => {
    return axios.post(API_URL, userData, { headers: authHeader() });
};

const updateUser = (id, userData) => {
    return axios.put(API_URL + "/" + id, userData, { headers: authHeader() });
};

const deactivateUser = (id) => {
    return axios.put(API_URL + "/" + id + "/deactivate", {}, { headers: authHeader() });
};

const userService = {
    getAllUsers,
    getUserById,
    getCurrentUser,
    createUser,
    updateUser,
    deactivateUser,
};

export default userService;
