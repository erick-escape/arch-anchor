import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
    test: {
        environment: 'node',
        setupFiles: ['./src/test/setup.ts'],
        // antd is slow under jsdom, and test files run in parallel on a busy CI runner. Under
        // jsdom 26, styling antd's elements took most of the time and tests ran 7-17 s; jsdom 29
        // matches selectors without nwsapi and keeps them near 1-2 s (#54).
        testTimeout: 15000,
        // Stylesheets are blank in tests unless included here; xyTheme.test.ts reads this one.
        css: { include: /xy-theme\.css/ },
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
