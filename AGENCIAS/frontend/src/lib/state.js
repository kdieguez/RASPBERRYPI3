import { writable } from "svelte/store";

export const vuelosItems   = writable([]);
export const vuelosLoading = writable(false);
export const vuelosError   = writable("");