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

    // solo actuamos en rutas protegidas
    if (status === 401 && url.startsWith("/api/v1/")) {
      // si YA no hay token (expiró o lo borraste), entonces sí redirige/login
      if (!getToken()) {
        clearAuth();
        if (!location.pathname.startsWith("/login")) location.assign("/login");
      }
      // si hay token vigente, NO borres sesión: deja que la pantalla muestre el error
    }

    return Promise.reject(err);
  }
);

export default axios;
