import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
    test: {
        environment: 'node',
        setupFiles: ['./src/test/setup.ts'],
        // antd is slow under jsdom, and test files run in parallel: a click through a
        // Popconfirm takes 4.7 s of the default 5 s on an idle machine, more on a busy one.
        testTimeout: 15000,
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
