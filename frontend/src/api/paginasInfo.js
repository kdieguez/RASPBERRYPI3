import axios from "../lib/axios";

export const paginasInfoApi = {
    list: () => axios.get("/api/public/paginas"),

    getById: (idPagina) => axios.get(`/api/public/paginas/${idPagina}`),

    getByName: (nombrePagina) =>
    axios.get(`/api/public/paginas/by-name/${encodeURIComponent(nombrePagina)}`),

    getAdmin: (idPagina) => axios.get(`/api/public/paginas/${idPagina}`),

    saveAdmin: (idPagina, payload) =>
    axios.put(`/api/v1/admin/paginas/${idPagina}`, payload),

    createSection: (idPagina, payload) =>
    axios.post(`/api/v1/admin/paginas/${idPagina}/secciones`, payload),

  updateSection: (idSeccion, payload) =>
    axios.put(`/api/v1/admin/secciones/${idSeccion}`, payload),

  deleteSection: (idSeccion) =>
    axios.delete(`/api/v1/admin/secciones/${idSeccion}`),

  reorderSections: (idPagina, items) =>
    axios.put(`/api/v1/admin/paginas/${idPagina}/secciones/reordenar`, items),

    createMedia: (idSeccion, payload) =>
    axios.post(`/api/v1/admin/secciones/${idSeccion}/media`, payload),

  deleteMedia: (idMedia) =>
    axios.delete(`/api/v1/admin/media/${idMedia}`),

  reorderMedia: (idSeccion, items) =>
    axios.put(`/api/v1/admin/secciones/${idSeccion}/media/reordenar`, items),
};

export default paginasInfoApi;
