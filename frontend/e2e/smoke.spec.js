import { test, expect } from '@playwright/test';

test('login page renders and shows welcome', async ({ page }) => {
  await page.goto('/login');
  await expect(page.locator('[data-message="welcome"]')).toBeVisible();
  await expect(page.locator('form#login-form')).toBeVisible();
});
