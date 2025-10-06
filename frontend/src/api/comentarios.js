import axios from "../lib/axios";

export const comentariosApi = {

  listPublic: (idVuelo) =>
    axios.get(`/api/public/vuelos/${idVuelo}/comentarios`),

  create: (idVuelo, dto) =>
    axios.post(`/api/v1/vuelos/${idVuelo}/comentarios`, dto),
};
