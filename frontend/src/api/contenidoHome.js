import axios from "../lib/axios";

export const tipsApi = {
    listPublic: () => axios.get("/api/public/tips"),

    listAdmin: () => axios.get("/api/v1/admin/tips"),
  create: (payload) => axios.post("/api/v1/admin/tips", payload),
  update: (idTip, payload) => axios.put(`/api/v1/admin/tips/${idTip}`, payload),
  remove: (idTip) => axios.delete(`/api/v1/admin/tips/${idTip}`),
};

export const noticiasApi = {
    listPublic: () => axios.get("/api/public/noticias"),

    listAdmin: () => axios.get("/api/v1/admin/noticias"),
  create: (payload) => axios.post("/api/v1/admin/noticias", payload),
  update: (idNoticia, payload) =>
    axios.put(`/api/v1/admin/noticias/${idNoticia}`, payload),
  remove: (idNoticia) =>
    axios.delete(`/api/v1/admin/noticias/${idNoticia}`),
};

export default {
  tipsApi,
  noticiasApi,
};
