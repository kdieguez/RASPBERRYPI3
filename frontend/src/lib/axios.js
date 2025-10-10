import _axios from "axios";
import { getToken, clearAuth } from "./auth";

const API = import.meta.env.VITE_API_URL || "http://localhost:8080";

const axios = _axios.create({ baseURL: API });

axios.interceptors.request.use((config) => {
  const t = getToken();
  if (t) config.headers.Authorization = `Bearer ${t}`;
  return config;
});

axios.interceptors.response.use(
  (r) => r,
  (err) => {
    const status = err?.response?.status;
    const url = err?.config?.url || "";

    if (status === 401 && url.startsWith("/api/v1/")) {
      if (!getToken()) {
        clearAuth();
        if (!location.pathname.startsWith("/login")) location.assign("/login");
      }

    }

    return Promise.reject(err);
  }
);

export default axios;
