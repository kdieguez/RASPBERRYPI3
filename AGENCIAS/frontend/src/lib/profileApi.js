// frontend/src/lib/profileApi.js
import { request } from "./api";

// GET perfil del usuario logueado
export function fetchProfile() {
  return request("/api/v1/perfil/me");
}

// PUT actualizar perfil
export function updateProfile(data) {
  return request("/api/v1/perfil/me", {
    method: "PUT",
    body: data,
  });
}