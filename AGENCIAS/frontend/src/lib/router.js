import { writable } from "svelte/store";

/* === Normalización === */
function normalize(p) {
  p = (p || "/").toString().replace(/^#/, "");
  if (!p.startsWith("/")) p = "/" + p;

  // alias amigables
  if (p === "/registro") p = "/register";
  if (p === "/ingresar" || p === "/iniciar-sesion") p = "/login";

  return p.replace(/\/{2,}/g, "/");
}
function getHashPath() {
  return normalize(window.location.hash.slice(1) || "/");
}

/* === Store de ruta === */
export const path = writable(getHashPath());
function apply() { path.set(getHashPath()); }

/* === Navegación programática (por si la usas) === */
export function navigate(to, opts = {}) {
  const next = normalize(to);
  const current = getHashPath();

  if (next !== current) {
    if (opts.replace) window.location.replace("#" + next);
    else window.location.hash = next;
  } else {
    path.set(next); // misma ruta: fuerza reacción
  }
  queueMicrotask(apply); // asegura sync inmediata
}

/* === Listeners globales === */
window.addEventListener("hashchange", apply);
window.addEventListener("popstate", apply);
window.addEventListener("load", apply);
queueMicrotask(apply); // tick inicial

/* === Helpers === */
const EMPTY_PARAMS = {};
export function match(pattern, currentPath) {
  const [pa] = splitPathAndQuery(normalize(pattern));
  const [pb] = splitPathAndQuery(normalize(currentPath));
  const A = pa.split("/").filter(Boolean);
  const B = pb.split("/").filter(Boolean);
  if (A.length !== B.length) return { ok: false, params: EMPTY_PARAMS };
  const params = {};
  for (let i = 0; i < A.length; i++) {
    if (A[i].startsWith(":")) params[A[i].slice(1)] = B[i];
    else if (A[i] !== B[i]) return { ok: false, params: EMPTY_PARAMS };
  }
  return { ok: true, params };
}
export function getQuery(currentPath) {
  const [, qs] = splitPathAndQuery(normalize(currentPath));
  return new URLSearchParams(qs);
}
function splitPathAndQuery(p) {
  const idx = p.indexOf("?");
  return idx >= 0 ? [p.slice(0, idx), p.slice(idx + 1)] : [p, ""];
}
export function onNavigate(cb) {
  const handler = () => cb(getHashPath());
  window.addEventListener("hashchange", handler);
  queueMicrotask(() => cb(getHashPath())); // emite estado actual
  return () => window.removeEventListener("hashchange", handler);
}