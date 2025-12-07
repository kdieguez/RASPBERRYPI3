import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const PORT =
  Number(process.env.PORT) ||
  Number(process.env.VITE_PORT) ||
  5174

export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    port: PORT,
    strictPort: false,
  },
})
