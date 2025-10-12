import { writable, derived, get } from "svelte/store";

const KEY = "auth";

function readFromStorage() {
  try {
    const raw = localStorage.getItem(KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw);

    const token =
      parsed?.token ?? parsed?.access_token ?? parsed?.accessToken ?? null;
    const user = parsed?.user ?? null;

    if (typeof token === "string" && token) {
      return { token, user: user || null };
    }
    localStorage.removeItem(KEY);
    return null;
  } catch {
    localStorage.removeItem(KEY);
    return null;
  }
}

const initial = readFromStorage();
export const auth = writable(initial);

auth.subscribe((val) => {
  try {
    if (!val) localStorage.removeItem(KEY);
    else localStorage.setItem(KEY, JSON.stringify(val));
  } catch {}
});

// ✅ Con token basta para estar logueado; user puede llegar luego con /auth/me
export const isLoggedIn = derived(auth, ($a) => !!($a && $a.token));
export const user = derived(auth, ($a) => $a?.user ?? null);

export function loadAuth() {
  const v = readFromStorage();
  auth.set(v);
  return v;
}

export function setAuth(value) {
  let next = null;
  if (value && typeof value === "object") {
    const token =
      value.token ?? value.access_token ?? value.accessToken ?? null;
    const usr = value.user ?? null;
    if (token) next = { token, user: usr || null };
  }
  auth.set(next);
  return next;
}

export function clearAuth() { auth.set(null); }
export function logout() { clearAuth(); }

export function getToken() { return get(auth)?.token ?? null; }
export function getUser() { return get(auth)?.user ?? null; }

export function updateUser(patch) {
  const current = get(auth);
  if (!current) return null;
  const nextUser =
    typeof patch === "function" ? patch(current.user) : { ...current.user, ...(patch || {}) };
  const next = { ...current, user: nextUser };
  auth.set(next);
  return next;
}

export function isAdmin() {
  const u = getUser();
  const r = u?.rol ?? u?.role ?? null;
  if (typeof r === "string") return r.trim().toLowerCase() === "admin";
  if (typeof r === "number") return r === 1;
  if (u?.is_admin === true) return true;
  return false;
}

export function isStaff() {
  const u = getUser();
  const r = u?.rol ?? u?.role ?? null;
  if (typeof r === "string") {
    const s = r.trim().toLowerCase();
    return s === "admin" || s === "empleado" || s === "staff";
  }
  if (typeof r === "number") return r === 1 || r === 2;
  return !!u?.is_staff;
}

export function authHeader() {
  const t = getToken();
  return t ? { Authorization: `Bearer ${t}` } : {};
}