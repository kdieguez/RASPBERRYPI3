import { writable } from 'svelte/store';

export const path = writable('/');

function normalize(p) {
  p = (p || '/').toString().replace(/^#/, '');
  if (!p.startsWith('/')) p = '/' + p;
  if (p === '/registro') p = '/register';
  if (p === '/ingresar' || p === '/iniciar-sesion') p = '/login';
  return p.replace(/\/{2,}/g, '/');
}

export function navigate(to) {
  const p = normalize(to);
  if (location.hash !== '#' + p) location.hash = p;
  else path.set(p);
}

function updateFromLocation() {
  const raw = location.hash.slice(1) || '/';
  path.set(normalize(raw));
}

window.addEventListener('hashchange', updateFromLocation);
updateFromLocation();

/** @type {Record<string, string>} */
const EMPTY_PARAMS = {};

/**
 * 
 * @param {string} pattern
 * @param {string} currentPath
 * @returns {{ ok: boolean, params: Record<string,string> }}
 */
export function match(pattern, currentPath) {
  const [pa] = splitPathAndQuery(normalize(pattern));
  const [pb] = splitPathAndQuery(normalize(currentPath));
  const A = pa.split('/').filter(Boolean);
  const B = pb.split('/').filter(Boolean);
  if (A.length !== B.length) return { ok: false, params: EMPTY_PARAMS };
  /** @type {Record<string,string>} */
  const params = {};
  for (let i = 0; i < A.length; i++) {
    if (A[i].startsWith(':')) params[A[i].slice(1)] = B[i];
    else if (A[i] !== B[i]) return { ok: false, params: EMPTY_PARAMS };
  }
  return { ok: true, params };
}

/**
 * 
 * @param {string} currentPath
 * @returns {URLSearchParams}
 */
export function getQuery(currentPath) {
  const [, qs] = splitPathAndQuery(normalize(currentPath));
  return new URLSearchParams(qs);
}

function splitPathAndQuery(p) {
  const idx = p.indexOf('?');
  return idx >= 0 ? [p.slice(0, idx), p.slice(idx + 1)] : [p, ''];
}

/**
 *
 * @param {(p:string)=>void} cb
 * @returns {() => void} 
 */
export function onNavigate(cb) {
  const handler = () => cb(normalize(location.hash.slice(1) || '/'));
  window.addEventListener('hashchange', handler);
  return () => window.removeEventListener('hashchange', handler);
}