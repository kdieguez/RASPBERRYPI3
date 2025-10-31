import _axios from "axios";
import { getToken, clearAuth } from "./auth";

function norm(u) {
  if (!u) return "";
  const s = String(u).trim();
  return s.endsWith("/") ? s.slice(0, -1) : s;
}

function resolveApiBase() {
  try {
    const params = new URLSearchParams(window.location.search || "");
    const q = params.get("api");
    if (q) {
      const v = norm(q);
      localStorage.setItem("apiBase", v);
      return v;
    }
  } catch {}

  if (window.__API_BASE__) return norm(window.__API_BASE__);

  const ls = localStorage.getItem("apiBase");
  if (ls) return norm(ls);

  const envBase = import.meta.env.VITE_API_BASE || import.meta.env.VITE_API_URL;
  if (envBase) return norm(envBase);

  return norm(window.location.origin);
}

const API_BASE = resolveApiBase();
console.info("[axios] API base:", API_BASE);

const axios = _axios.create({ baseURL: API_BASE });

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


    if (status === 401 && url.startsWith("/api/")) {
      clearAuth();
      if (!location.pathname.startsWith("/login")) location.assign("/login");
    }

    return Promise.reject(err);
  }
);

export default axios;
