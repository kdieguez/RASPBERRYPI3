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
  path.set(normalize(location.hash.slice(1) || '/'));
}

window.addEventListener('hashchange', updateFromLocation);
updateFromLocation();