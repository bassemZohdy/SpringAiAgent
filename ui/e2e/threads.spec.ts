import { test, expect } from '@playwright/test';

test.describe('Threads sidebar', () => {
  test('renders threads and allows selection', async ({ page }) => {
    await page.route('**/v1/threads', async route => {
      if (route.request().method() === 'GET') {
        const now = Math.floor(Date.now() / 1000);
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            object: 'list',
            data: [
              { id: 't1', object: 'thread', created_at: now - 500, title: 'First chat', message_count: 3, last_activity: now - 300 },
              { id: 't2', object: 'thread', created_at: now - 200, title: 'Second chat', message_count: 1, last_activity: now - 100 }
            ]
          })
        });
      }
      return route.continue();
    });

    await page.route('**/v1/threads/*/messages', async route => {
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ object: 'list', data: [] }) });
    });

    await page.route('**/v1/models', async route => {
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ object: 'list', data: [] }) });
    });

    await page.goto('/');

    await expect(page.locator('.thread-list-title')).toBeVisible();
    const items = page.locator('.thread-item');
    await expect(items).toHaveCount(2, { timeout: 10000 });
    await expect(items.nth(0)).toContainText('First chat');
    await expect(items.nth(1)).toContainText('Second chat');

    // Select second thread and expect active class
    await items.nth(1).click();
    await expect(items.nth(1)).toHaveClass(/active/, { timeout: 5000 });
  });
});
