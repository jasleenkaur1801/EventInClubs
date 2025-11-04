import axios from "axios";

// Allow overriding the API base URL via environment variable, fall back to local backend
const API_BASE = import.meta.env.VITE_API_BASE ?? "http://localhost:8080/api";

const http = axios.create({
  baseURL: API_BASE,
  headers: {
    "Content-Type": "application/json",
  },
  withCredentials: true
});

// Add request interceptor for logging
http.interceptors.request.use(config => {
  console.log(`Request: ${config.method.toUpperCase()} ${config.baseURL}${config.url}`);
  return config;
}, error => {
  console.error('Request Error:', error);
  return Promise.reject(error);
});

// Add response interceptor for logging
http.interceptors.response.use(response => {
  console.log(`Response: ${response.status} ${response.config.method.toUpperCase()} ${response.config.url}`);
  return response;
}, error => {
  console.error('Response Error:', {
    status: error.response?.status,
    url: error.config?.url,
    method: error.config?.method,
    data: error.response?.data
  });
  return Promise.reject(error);
});

export default http;
export { http as httpClient };