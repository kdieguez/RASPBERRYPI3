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
    u?.idUsuario ??
    u?.id ??
    u?.userId ??
    u?.ID_Usuario ??
    u?.userid;
  if (uid) h["X-User-Id"] = String(uid);

  const email =
    u?.email ??
    u?.correo ??
    u?.mail ??
    u?.Email ??
    u?.Correo ??
    u?.EMAIL;
  if (email && String(email).includes("@")) {
    h["X-User-Email"] = String(email).trim();
  }

  const name = 
    u?.nombre ?? 
    u?.name ?? 
    u?.fullName ?? 
    u?.usuario ?? 
    u?.username;
  if (name && String(name).trim()) h["X-User-Name"] = String(name).trim();

  return h;
}

export const comprasApi = {
  getCart() {
    return api.get("/api/compras/carrito", { headers: authHeaders() });
  },

  addItem({ idVuelo, idClase, cantidad = 1 }) {
    return api.post(
      "/api/compras/items",
      { idVuelo, idClase, cantidad },
      { headers: authHeaders() }
    );
  },

  updateItem(idItem, cantidad) {
    return api.put(
      `/api/compras/items/${idItem}`,
      { cantidad },
      { headers: authHeaders() }
    );
  },

  removeItem(idItem) {
    return api.delete(`/api/compras/items/${idItem}`, {
      headers: authHeaders(),
    });
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
    return api.post(`/api/compras/reservas/${id}/cancelar`, {}, { headers: authHeaders() });
  },

  downloadBoleto(id) {
    return api.get(`/api/compras/reservas/${id}/boleto.pdf`, {
      headers: authHeaders(),
      responseType: "blob",
    });
  },
};
