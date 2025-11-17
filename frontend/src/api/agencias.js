import axios from "../lib/axios";

export const agenciasApi = {
  list: (soloHabilitadas = false) =>
    axios.get(`/api/admin/agencias${soloHabilitadas ? "?soloHabilitadas=true" : ""}`),
  get: (id) => axios.get(`/api/admin/agencias/${encodeURIComponent(id)}`),
  create: (body) => axios.post(`/api/admin/agencias`, body),
  update: (id, body) => axios.put(`/api/admin/agencias/${encodeURIComponent(id)}`, body),
  remove: (id) => axios.delete(`/api/admin/agencias/${encodeURIComponent(id)}`),
};




