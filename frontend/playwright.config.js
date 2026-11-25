import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  retries: process.env.CI ? 1 : 0,
  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:8080',
    trace: 'on-first-retry',
  },
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  projects: [{ name: 'chromium', use: { browserName: 'chromium' } }],
});
