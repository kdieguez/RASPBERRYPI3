import { writable } from "svelte/store";

function normalize(p) {
  p = (p || "/").toString().trim();
  if (p.startsWith("#")) p = p.slice(1);
  if (!p.startsWith("/")) p = "/" + p;
  if (p === "/registro") p = "/register";
  if (p === "/ingresar" || p === "/iniciar-sesion") p = "/login";
  return p.replace(/\/{2,}/g, "/");
}
function splitPathAndQuery(p) {
  const idx = p.indexOf("?");
  return idx >= 0 ? [p.slice(0, idx), p.slice(idx + 1)] : [p, ""];
}
function currentPathOnly() {
  return normalize(location.pathname || "/");
}

if ((location.hash || "").startsWith("#/")) {
  history.replaceState({}, "", normalize(location.hash.slice(1)));
}

export const path = writable(currentPathOnly());
function apply() { path.set(currentPathOnly()); }

export function navigate(to, opts = {}) {
  const next = normalize(to);
  const curr = currentPathOnly();
  if (next !== curr) {
    const fn = opts.replace ? "replaceState" : "pushState";
    history[fn]({}, "", next);
  } else {
    path.set(next);
  }
  queueMicrotask(apply);
}

window.addEventListener("popstate", apply);
window.addEventListener("load", apply);
window.addEventListener("hashchange", () => {
  if ((location.hash || "").startsWith("#/")) {
    history.replaceState({}, "", normalize(location.hash.slice(1)));
    apply();
  }
});
queueMicrotask(apply);

const EMPTY_PARAMS = {};
export function match(pattern, current) {
  const [pa] = splitPathAndQuery(normalize(pattern));
  const [pb] = splitPathAndQuery(normalize(current || currentPathOnly()));
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

export function getQuery(current) {
  if (typeof current === "string" && current.length) {
    const [, qs] = splitPathAndQuery(normalize(current));
    return new URLSearchParams(qs);
  }
  return new URLSearchParams((location.search || "").replace(/^\?/, ""));
}

export function onNavigate(cb) {
  const unsub = path.subscribe((p) => cb(p));
  queueMicrotask(() => cb(currentPathOnly()));
  return () => unsub();
}

export function link(node) {
  function onClick(e) {
    if (e.defaultPrevented || e.button !== 0) return;
    if (e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return;

    const a = e.target.closest("a");
    if (!a || a !== node) return;

    const href = a.getAttribute("href");
    if (!href) return;
    if (a.target === "_blank") return;
    if (href.startsWith("mailto:") || href.startsWith("tel:")) return;
    if (/^https?:\/\//i.test(href)) return;
    if (href.startsWith("#")) return;    

    e.preventDefault();
    navigate(href);
  }
  node.addEventListener("click", onClick);
  return { destroy() { node.removeEventListener("click", onClick); } };
}

export function navform(node) {
  function onSubmit(e) {
    const method = (node.getAttribute("method") || "GET").toUpperCase();
    if (method !== "GET") return; 
    e.preventDefault();

    const action = node.getAttribute("action") || location.pathname;
    const fd = new FormData(node);
    const qs = new URLSearchParams(fd);
    const url = qs.toString() ? `${action}?${qs}` : action;
    navigate(url);
  }
  node.addEventListener("submit", onSubmit);
  return { destroy() { node.removeEventListener("submit", onSubmit); } };
}