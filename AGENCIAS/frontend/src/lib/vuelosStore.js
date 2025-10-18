import { writable } from "svelte/store";
import { VuelosAPI } from "@/lib/api";

export const vuelosItems   = writable([]);    
export const vuelosLoading = writable(false); 
export const vuelosError   = writable("");    

export async function loadVuelos({ origin = "", destination = "", date = "", pax = 1 } = {}) {
  vuelosLoading.set(true);
  vuelosError.set("");
  try {
    const res = await VuelosAPI.search({ origin, destination, date, pax });
    vuelosItems.set(Array.isArray(res) ? res : []);
  } catch (e) {
    vuelosError.set(e?.message || "Error al buscar vuelos");
    vuelosItems.set([]);
  } finally {
    vuelosLoading.set(false);
  }
}