import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
    plugins: [react(), tailwindcss()],
    server: {
        // Dev proxy: forwards /api requests to the local Spring Boot backend.
        // In production (split-host), set VITE_API_BASE_URL=https://your-backend.render.com
        // in a .env.production file (or your hosting platform's env vars).
        // The frontend code reads import.meta.env.VITE_API_BASE_URL, defaulting to ""
        // (same origin) when the variable is absent — which is correct for both
        // local dev (where Vite proxies) and same-host production deploys.
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
            }
        }
    }
})