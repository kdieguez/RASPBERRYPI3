import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const PORT =
  Number(process.env.PORT) ||
  Number(process.env.VITE_PORT) ||
  5173

export default defineConfig({
  plugins: [react()],
  server: {
    host: true,          
    port: PORT,          
    strictPort: false,   
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setupTests.js',
    globals: true,
    include: ['src/**/*.{test,spec}.{js,jsx,ts,tsx}']
  }
})
