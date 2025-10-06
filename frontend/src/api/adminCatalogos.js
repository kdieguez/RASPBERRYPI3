import axios from "../lib/axios";

export const paisesApi = {
  create: (nombre) => axios.post("/api/v1/paises", { nombre }),
  list:   ()        => axios.get ("/api/public/paises"),
};

export const ciudadesApi = {
  create: (idPais, nombre) => axios.post("/api/v1/ciudades", { idPais, nombre }),
  list:   (idPais)         => axios.get("/api/public/ciudades", { params: { idPais } }),
};

export const rutasApi = {
  create: (idCiudadOrigen, idCiudadDestino) =>
    axios.post("/api/v1/rutas", { idCiudadOrigen, idCiudadDestino }),
  list: () => axios.get("/api/v1/rutas"),
};

export const clasesApi = {
  list: () => axios.get("/api/public/clases"),
};

export const vuelosApi = {
  listPublic: () => axios.get("/api/public/vuelos"),
  listAdmin:  () => axios.get("/api/v1/admin/vuelos"),

  getPublic: (id) => axios.get(`/api/public/vuelos/${id}`),

  getAdmin:     (id)      => axios.get(`/api/v1/admin/vuelos/${id}`),
  updateAdmin:  (id, dto) => axios.put(`/api/v1/admin/vuelos/${id}`, dto),
  createAdmin:  (dto)     => axios.post("/api/v1/admin/vuelos", dto),

  create:          (dto)  => axios.post("/api/v1/vuelos", dto),
  createRoundTrip: (dto)  => axios.post("/api/v1/vuelos/roundtrip", dto),
  link:            (body) => axios.post("/api/v1/vuelos/link", body),

  updateEstado: (id, idEstado, motivo) =>
    axios.put(`/api/v1/vuelos/${id}/estado`, { idEstado, motivo }),
};
