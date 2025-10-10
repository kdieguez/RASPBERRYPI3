import { getToken } from "./auth";

const BASE = import.meta.env.VITE_API_BASE || "http://localhost:8001";

async function withAuthFetch(url, opts = {}) {
  const t = getToken();
  const headers = {
    "Content-Type": "application/json",
    ...(opts.headers || {}),
    ...(t ? { Authorization: `Bearer ${t}` } : {}),
  };

  const r = await fetch(url, { ...opts, headers });
  if (!r.ok) {
    let msg = `HTTP ${r.status}`;
    try { msg = (await r.json())?.detail || msg; } catch {}
    throw new Error(msg);
  }
  return r.json();
}

export const fetchUI = () =>
  withAuthFetch(`${BASE}/portal/ui`, { method: "GET" });

export const saveHeader = (payload) =>
  withAuthFetch(`${BASE}/portal/header`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });

export const saveFooter = (payload) =>
  withAuthFetch(`${BASE}/portal/footer`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });