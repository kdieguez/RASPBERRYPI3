// vite.config.js
import { defineConfig } from "vite";
import { svelte } from "@sveltejs/vite-plugin-svelte";
import path from "path";

export default defineConfig({
  plugins: [svelte()],
  appType: "spa",
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "src"), // <-- alias único para 'src'
    },
  },
  server: { port: 5173 },
  preview: { port: 4173 },
});
