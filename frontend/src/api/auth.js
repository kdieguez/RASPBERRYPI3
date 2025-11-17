import axios from "../lib/axios";

export const authApi = {
  register: (body) => axios.post("/api/auth/register", body),
};




