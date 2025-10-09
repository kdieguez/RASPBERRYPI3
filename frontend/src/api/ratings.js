import axios from "../lib/axios";
export const ratingsApi = {
  resumen: (idVuelo) => axios.get(`/api/public/vuelos/${idVuelo}/ratings/resumen`),
  create:  (idVuelo, body) => axios.post(`/api/v1/vuelos/${idVuelo}/ratings`, body),
};
