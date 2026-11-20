import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const backendPublic = fileURLToPath(
  new URL('../src/main/resources/public/react-assets', import.meta.url),
);

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/booking': 'http://localhost:8080',
      '/css': 'http://localhost:8080',
      '/js': 'http://localhost:8080',
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
      },
    },
  },
});
