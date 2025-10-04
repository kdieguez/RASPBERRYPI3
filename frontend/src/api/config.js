import axios from "../lib/axios";

export const configApi = {
  getHeader: async () => (await axios.get("/api/config/header")).data,
  getFooter: async () => (await axios.get("/api/config/footer")).data,

  updateHeader: (body) => axios.put("/api/admin/config/header", body),
  updateFooter: (body) => axios.put("/api/admin/config/footer", body),

  upsertSection: (section, body) =>
    axios.put(`/api/admin/config/${section}`, body),
};
