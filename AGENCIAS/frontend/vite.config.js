// vite.config.js
import { defineConfig } from "vite";
import { svelte } from "@sveltejs/vite-plugin-svelte";
import path from "path";

const PORT = Number(process.env.PORT || process.env.VITE_PORT || 5174);

export default defineConfig({
  plugins: [svelte()],
  appType: "spa",
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "src"), // <-- alias único para 'src'
    },
  },
  server: { port: PORT },
  preview: { port: 4173 },
});
