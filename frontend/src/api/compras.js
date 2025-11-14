import api from "../lib/axios";
import { getUser, getToken } from "../lib/auth";

function authHeaders() {
  const u = getUser();
  const t = getToken();

  const h = {
    "Content-Type": "application/json",
    Accept: "application/json",
  };

  if (t) h.Authorization = `Bearer ${t}`;

  const uid =
    u?.idUsuario ?? u?.id ?? u?.userId ?? u?.ID_Usuario ?? u?.userid;
  if (uid) h["X-User-Id"] = String(uid);

  const email =
    u?.email ?? u?.correo ?? u?.mail ?? u?.Email ?? u?.Correo ?? u?.EMAIL;
  if (email && String(email).includes("@"))
    h["X-User-Email"] = String(email).trim();

  const name =
    u?.nombre ?? u?.name ?? u?.fullName ?? u?.usuario ?? u?.username;
  if (name && String(name).trim()) h["X-User-Name"] = String(name).trim();

  return h;
}

const qs = (obj = {}) => {
  const p = new URLSearchParams();
  Object.entries(obj).forEach(([k, v]) => {
    if (v === true) p.set(k, "true");
    if (v && v !== true) p.set(k, String(v));
  });
  const s = p.toString();
  return s ? `?${s}` : "";
};

export const comprasApi = {
  getCart() {
    return api.get("/api/compras/carrito", { headers: authHeaders() });
  },

  addItem({ idVuelo, idClase, cantidad = 1 }, opts = {}) {
    return api.post(
      `/api/compras/items${qs({ pair: opts.pair })}`,
      { idVuelo, idClase, cantidad },
      { headers: authHeaders() }
    );
  },

  updateItem(idItem, cantidad, opts = {}) {
    return api.put(
      `/api/compras/items/${idItem}${qs({ syncPareja: opts.syncPareja })}`,
      { cantidad },
      { headers: authHeaders() }
    );
  },

  removeItem(idItem, opts = {}) {
    return api.delete(
      `/api/compras/items/${idItem}${qs({ syncPareja: opts.syncPareja })}`,
      { headers: authHeaders() }
    );
  },

  checkout(payload) {
    return api.post("/api/compras/checkout", payload || {}, {
      headers: authHeaders(),
    });
  },

  listReservas() {
    return api.get("/api/compras/reservas", { headers: authHeaders() });
  },

  getReserva(id) {
    return api.get(`/api/compras/reservas/${id}`, { headers: authHeaders() });
  },

  cancelReserva(id) {
    return api.post(
      `/api/compras/reservas/${id}/cancelar`,
      {},
      { headers: authHeaders() }
    );
  },

  downloadBoleto(id) {
    return api.get(`/api/compras/reservas/${id}/boleto.pdf`, {
      headers: authHeaders(),
      responseType: "blob",
    });
  },

  adminListReservas(params) {
    return api.get("/api/admin/reservas", {
      headers: authHeaders(),
      params,
    });
  },

  adminGetReserva(id) {
    return api.get(`/api/admin/reservas/${id}`, { headers: authHeaders() });
  },

  adminListEstados() {
    return api.get("/api/admin/reservas/estados", { headers: authHeaders() });
  },

  topDestinos(params) {
    return api.get("/api/public/stats/top-destinos", { params });
  },
};
