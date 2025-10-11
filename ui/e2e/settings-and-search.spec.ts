import { test, expect } from '@playwright/test';

async function mockApi(page: import('@playwright/test').Page) {
  await page.route('**/v1/threads', async route => {
    const req = route.request();
    if (req.method() === 'GET') {
      const now = Math.floor(Date.now() / 1000);
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ object: 'list', data: [ { id: 't1', object: 'thread', created_at: now, title: 'Demo', message_count: 0, last_activity: now } ] })
      });
    }
    if (req.method() === 'POST') {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 'thread_x',
          object: 'thread',
          created_at: Math.floor(Date.now() / 1000),
          title: 'New Smart Chat',
          message_count: 0,
          last_activity: Math.floor(Date.now() / 1000)
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

  await page.route('**/v1/chat/completions', async route => {
    const headers = route.request().headers();
    const accept = headers['accept'] || headers['Accept'] || '';
    if (accept.includes('text/event-stream')) {
      const chunk = {
        id: 'cmpl_x',
        object: 'chat.completion.chunk',
        created: Math.floor(Date.now() / 1000),
        model: 'gpt-4o-mini',
        choices: [{ index: 0, delta: { content: 'Mocked reply' } }]
      };
      const body = `data: ${JSON.stringify(chunk)}\n\n` + 'data: [DONE]\n\n';
      return route.fulfill({ status: 200, contentType: 'text/event-stream', headers: { 'Cache-Control': 'no-cache' }, body });
    }
    const response = {
      id: 'cmpl_x',
      object: 'chat.completion',
      created: Math.floor(Date.now() / 1000),
      model: 'gpt-4o-mini',
      choices: [
        { index: 0, message: { role: 'assistant', content: 'Mocked reply' }, finish_reason: 'stop' }
      ],
      usage: { prompt_tokens: 0, completion_tokens: 0, total_tokens: 0 }
    };
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(response) });
  });
}

test.describe('Settings and search', () => {
  test('toggles dark mode from settings', async ({ page }) => {
    await mockApi(page);
    await page.goto('/');

    const card = page.locator('.smart-chat-card');
    await expect(card).toBeVisible();
    await expect(card).not.toHaveClass(/dark-theme/);

    // Open the chat settings menu (icon within the chat header)
    const settingsBtn = page.locator('.smart-chat-header .header-actions button', { has: page.locator('mat-icon', { hasText: 'settings' }) }).first();
    await settingsBtn.click();
    await page.getByRole('menuitem', { name: /Mode/ }).click();

    await expect(card).toHaveClass(/dark-theme/);
  });

  test('search filters messages', async ({ page }) => {
    await mockApi(page);
    await page.goto('/');

    // Select existing thread to avoid async thread creation path
    const threadItems = page.locator('.thread-item');
    await expect(threadItems).toHaveCount(1, { timeout: 10000 });
    await threadItems.first().click();

    // Disable streaming so input doesn't stay disabled between sends
    const settingsBtn = page.locator('.smart-chat-header .header-actions button', { has: page.locator('mat-icon', { hasText: 'settings' }) }).first();
    await settingsBtn.click();
    await page.getByRole('menuitem', { name: /Streaming/ }).click();

    // No need to toggle streaming; mocks handle both JSON and SSE paths

    // Send two distinct messages
    const input = page.locator('textarea[placeholder="Ask me anything..."]');

    await input.fill('alpha topic');
    await page.locator('button.send-button').click();
    await expect(page.locator('.message-wrapper.assistant-message')).toHaveCount(1);

    await input.fill('beta subject');
    await page.locator('button.send-button').click();
    await expect(page.locator('.message-wrapper.assistant-message')).toHaveCount(2);

    // Open search and filter (search icon button in chat header)
    const searchBtn = page.locator('.smart-chat-header .header-actions button', { has: page.locator('mat-icon', { hasText: 'search' }) }).first();
    await searchBtn.click();
    const searchField = page.getByPlaceholder('Search messages...');
    await searchField.fill('alpha');

    // Only messages containing "alpha" remain
    const visibleMessages = page.locator('.message-wrapper');
    await expect(visibleMessages).toHaveCount(1); // user only for alpha

    await searchField.fill('beta');
    await expect(visibleMessages).toHaveCount(1); // user only for beta

    await searchField.fill('nomatch');
    await expect(visibleMessages).toHaveCount(0);
  });
});
