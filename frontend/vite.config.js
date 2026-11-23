import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const backendPublic = fileURLToPath(
  new URL('../src/main/resources/public/react-assets', import.meta.url),
);

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './tests/setup.js',
    include: ['src/**/*.{test,spec}.{js,jsx}'],
    css: false,
    pool: 'vmThreads',
  },
  server: {
    proxy: {
      '/booking': 'http://localhost:8080',
      '/csrf': 'http://localhost:8080',
      '/css': 'http://localhost:8080',
      '/js': 'http://localhost:8080',
      '/login': 'http://localhost:8080',
      '/logout': 'http://localhost:8080',
    },
  },
  build: {
    outDir: backendPublic,
    emptyOutDir: false,
    minify: 'esbuild',
    rollupOptions: {
      input: fileURLToPath(new URL('./src/main.jsx', import.meta.url)),
      output: {
        entryFileNames: 'main.js',
        chunkFileNames: '[name].js',
        assetFileNames: '[name][extname]',
        manualChunks(id) {
          if (id.includes('node_modules')) {
            if (id.includes('react-dom') || id.includes('/react/')) return 'vendor-react';
            if (id.includes('i18next')) return 'vendor-i18n';
            return 'vendor';
          }
        },
      },
    },
  },
});
