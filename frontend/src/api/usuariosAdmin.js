import axios from "../lib/axios";

export const adminUsuariosApi = {
  list: () => axios.get("/api/admin/usuarios"),
  get: (id) => axios.get(`/api/admin/usuarios/${id}`),
  update: (id, body) => axios.put(`/api/admin/usuarios/${id}`, body),
  createWS: (body) => axios.post("/api/admin/usuarios/create-ws", body),
};


