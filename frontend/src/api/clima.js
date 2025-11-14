import axios from "../lib/axios";

export const climaApi = {
  listCities: () => axios.get("/api/public/clima/ciudades"),
};

export default climaApi;
