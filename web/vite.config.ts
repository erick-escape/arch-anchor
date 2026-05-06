import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
    test: {
        environment: 'node',
    },
    plugins: [react()],
    server: {
        proxy: {
            '/api': {
                target: 'http://localhost:8080', // Spring Boot backend URL
                changeOrigin: true
            }
        }
    }
});
