const BASE = import.meta.env.VITE_API_URL;

/**
 * @param {string} path
 * @param {{ method?: 'GET'|'POST'|'PUT'|'PATCH'|'DELETE', body?: any, headers?: Record<string,string>, timeout?: number }} [opts]
 */
export async function request(path, opts = {}) {
  const { method = 'GET', body, headers, timeout = 10000 } = opts;

  let token;
  try {
    const raw = localStorage.getItem('auth');
    token = raw ? JSON.parse(raw)?.token : undefined;
  } catch {}

  const controller = new AbortController();
  const id = setTimeout(() => controller.abort(), timeout);

  const res = await fetch((BASE || '') + path, {
    method,
    headers: {
      Accept: 'application/json',
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(headers || {}),
    },
    body: body ? JSON.stringify(body) : undefined,
    signal: controller.signal,
  }).catch((e) => {
    clearTimeout(id);
    throw new Error(e?.message || 'No se pudo conectar con el servidor');
  });

  clearTimeout(id);

  if (res.status === 204) return null;

  const text = await res.text();
  let json = null;
  try {
    json = text ? JSON.parse(text) : null;
  } catch {
    json = null;
  }

  if (!res.ok) {
    const msg =
      (json && (json.message || json.detail)) ||
      text ||
      `HTTP ${res.status} ${res.statusText}`;
    throw new Error(msg);
  }
  return json ?? { ok: true, raw: text };
}

export function qs(obj = {}) {
  const p = new URLSearchParams();
  for (const [k, v] of Object.entries(obj)) {
    if (v === undefined || v === null || String(v).trim() === '') continue;
    p.set(k, String(v));
  }
  const s = p.toString();
  return s ? `?${s}` : '';
}

export const AuthAPI = {
  register: (data) => request('/api/v1/auth/register', { method: 'POST', body: data }),
  login:  (data) => request('/api/v1/auth/login',    { method: 'POST', body: data }),
  me:     ()     => request('/api/v1/auth/me'),
};

export const UsersAPI = {
  list:   (params)   => request('/api/v1/users' + qs(params)),
  get:    (id)       => request(`/api/v1/users/${id}`),
  create: (body)     => request('/api/v1/users', { method: 'POST', body }),
  update: (id, body) => request(`/api/v1/users/${id}`, { method: 'PATCH', body }),
  remove: (id)       => request(`/api/v1/users/${id}`, { method: 'DELETE' }),
};

/** @typedef {{origin?: string, destination?: string, date?: string, pax?: number}} SearchParams */

export const VuelosAPI = {
  search: async (params = {}) => {
    const { origin, destination, date, pax } = params;
    const res = await request('/vuelos' + qs({ origin, destination, date, pax }));
    if (Array.isArray(res)) return res;
    if (res && Array.isArray(res.items)) return res.items;
    if (res && Array.isArray(res.results)) return res.results;
    return [];
  },
  detail:       (id, pax = 1) => request(`/vuelos/${id}` + qs({ pax })),
  origins:      ()            => request('/vuelos/origins'),
  destinations: (origin)      => request('/vuelos/destinations' + qs({ origin })),
};

export const ComprasAPI = {
  // Cliente normal
  getCart: () => request('/compras/carrito'),

  addItem: ({ idVuelo, idClase, cantidad = 1, pair = false, proveedor }) =>
    request(`/compras/items?pair=${pair ? 'true' : 'false'}`, {
      method: 'POST',
      body: { idVuelo, idClase, cantidad, ...(proveedor ? { proveedor } : {}) },
    }),

  updateQty: (idItem, cantidad, syncPareja = false) =>
    request(`/compras/items/${idItem}?syncPareja=${syncPareja ? 'true' : 'false'}`, {
      method: 'PUT',
      body: { cantidad },
    }),

  removeItem: (idItem, syncPareja = false) =>
    request(`/compras/items/${idItem}?syncPareja=${syncPareja ? 'true' : 'false'}`, {
      method: 'DELETE',
    }),

  checkout: (payment) =>
    request('/compras/checkout', { method: 'POST', body: payment }),

  list:   ()    => request('/compras/reservas'),
  detail: (id)  => request(`/compras/reservas/${id}`),

  cancel: (id) =>
    request(`/compras/reservas/${id}/cancelar`, {
      method: 'POST',
    }),

  boletoPdf: async (id) => {
    const BASEURL = import.meta.env.VITE_API_URL || '';
    let token;
    try {
      const raw = localStorage.getItem('auth');
      token = raw ? JSON.parse(raw)?.token : undefined;
    } catch {}
    const res = await fetch(`${BASEURL}/compras/reservas/${id}/boleto.pdf`, {
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    });
    if (!res.ok) {
      const t = await res.text();
      throw new Error(t || `HTTP ${res.status}`);
    }
    return await res.blob();
  },

  listAdmin:   ()   => request('/compras/admin/reservas'),
  detailAdmin: (id) => request(`/compras/admin/reservas/${id}`),

  cancelAdmin: (id) =>
    request(`/compras/admin/reservas/${id}/cancelar`, {
      method: 'POST',
    }),
};

export const PaginasAPI = {
  // Público
  getPublic: (slug) =>
    request(`/api/public/paginas/${encodeURIComponent(slug)}`),

  // Listados y detalle (admin)
  listAdmin: (params) =>
    request('/api/admin/paginas' + qs(params || {})),

  getAdmin: (slug) =>
    request(`/api/admin/paginas/${encodeURIComponent(slug)}`),

  // Crear nueva página informativa
  create: (body) =>
    request('/api/admin/paginas', {
      method: 'POST',
      body,
    }),

  createAdmin: (body) =>
    request('/api/admin/paginas', {
      method: 'POST',
      body,
    }),

  // Actualizar página existente
  update: (slug, body) =>
    request(`/api/admin/paginas/${encodeURIComponent(slug)}`, {
      method: 'PUT',
      body,
    }),

  updateAdmin: (slug, body) =>
    request(`/api/admin/paginas/${encodeURIComponent(slug)}`, {
      method: 'PUT',
      body,
    }),

  // Eliminar página
  remove: (slug) =>
    request(`/api/admin/paginas/${encodeURIComponent(slug)}`, {
      method: 'DELETE',
    }),

  removeAdmin: (slug) =>
    request(`/api/admin/paginas/${encodeURIComponent(slug)}`, {
      method: 'DELETE',
    }),
};

export function toDate(v) {
  if (v == null) return null;
  if (v instanceof Date) return isNaN(v) ? null : v;
  if (typeof v === 'number') return new Date(v > 1e12 ? v : v * 1000);
  if (typeof v === 'string') {
    let s = v.trim();

    if (/^\d{4}-\d{2}-\d{2}/.test(s)) {
      const d = new Date(s.replace(' ', 'T'));
      if (!isNaN(d)) return d;
    }

    const m = s.match(
      /^(\d{2})-([A-Z]{3})-(\d{2,4})\s+(\d{2})\.(\d{2})\.(\d{2})/i
    );
    if (m) {
      const [_, dd, mon, yy, hh, mm, ss] = m;
      const months = {
        JAN: 0,
        FEB: 1,
        MAR: 2,
        APR: 3,
        MAY: 4,
        JUN: 5,
        JUL: 6,
        AUG: 7,
        SEP: 8,
        OCT: 9,
        NOV: 10,
        DEC: 11,
      };
      const year = Number(yy) < 100 ? 2000 + Number(yy) : Number(yy);
      return new Date(
        year,
        months[mon.toUpperCase()] ?? 0,
        Number(dd),
        Number(hh),
        Number(mm),
        Number(ss)
      );
    }

    const d = new Date(s);
    if (!isNaN(d)) return d;
  }
  return null;
}

export function fmtDate(d, opts = {}) {
  if (!d) return '';
  return new Intl.DateTimeFormat('es-GT', {
    year: 'numeric',
    month: 'short',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    ...opts,
  }).format(d);
}