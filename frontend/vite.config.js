const PORT =
  Number(process.env.PORT) ||
  Number(process.env.VITE_PORT) ||
  5173;

export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    port: PORT,
    // strictPort: false -> si el puerto está ocupado, Vite tomará otro libre
    strictPort: false,
  },
});