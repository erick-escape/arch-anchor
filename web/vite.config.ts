import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
    test: {
        environment: 'node',
        setupFiles: ['./src/test/setup.ts'],
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
