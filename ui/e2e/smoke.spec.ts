import { test, expect } from '@playwright/test';

test.describe('Spring AI Agent UI smoke', () => {
  test('loads shell and core elements', async ({ page }) => {
    await page.goto('/');

    // App title in toolbar
    await expect(page.getByText('Spring AI Agent Chat')).toBeVisible();

    // Chat card title (exact to avoid matching welcome header)
    await expect(page.getByText('Smart AI Chat', { exact: true })).toBeVisible();

    // Welcome message appears when no messages
    await expect(page.getByText('Welcome to Smart AI Chat')).toBeVisible();

    // Message input field present
    await expect(page.getByLabel('Type your message...')).toBeVisible();

    // Underlying textarea placeholder also present
    await expect(page.locator('textarea[placeholder="Ask me anything..."]')).toBeVisible();
  });
});
