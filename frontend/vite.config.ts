import path from 'node:path'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(import.meta.dirname, './src'),
    },
  },
  // Leaflet entra só por import dinâmico (mapa da localização). Sem pré-empacotar, o Vite descobre a dependência na
  // primeira abertura do mapa e recarrega a página no meio do uso (e do teste E2E). Ver docs/testes-instaveis.md.
  optimizeDeps: {
    include: ['leaflet', 'react-leaflet'],
  },
  // Portas configuráveis para conviver com outros serviços locais (ver .env.example).
  server: {
    port: Number(process.env.FRONTEND_PORT ?? 5173),
    strictPort: true,
    proxy: {
      '/api': process.env.API_URL ?? 'http://localhost:8080',
    },
  },
})
