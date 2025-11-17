import { writable } from 'svelte/store';

export const proveedorActual = writable(null);

export function setProveedor(proveedor) {
  if (proveedor) {
    proveedorActual.set(proveedor);
    try {
      localStorage.setItem('vuelo_detalle_proveedor', proveedor);
    } catch {
    }
  }
}

export function getProveedor() {
  let prov = null;
  const unsubscribe = proveedorActual.subscribe(value => {
    prov = value;
  });
  unsubscribe();
  
  if (!prov) {
    try {
      prov = localStorage.getItem('vuelo_detalle_proveedor');
    } catch {
    }
  }
  
  return prov;
}

export function clearProveedor() {
  proveedorActual.set(null);
  try {
    localStorage.removeItem('vuelo_detalle_proveedor');
  } catch {
  }
}


