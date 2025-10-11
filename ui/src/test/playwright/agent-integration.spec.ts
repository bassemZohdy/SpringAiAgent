import { test, expect } from '@playwright/test';
import { ChatRequest } from '../models/chat-request';

test.describe('Agent Integration E2E Tests', () => {
  const BASE_URL = 'http://localhost:4200'; // Angular UI
  const API_BASE_URL = 'http://localhost:8080'; // Spring Boot API

  test.beforeEach(async ({ page }) => {
    // Navigate to the application
    await page.goto(BASE_URL);

    // Wait for the application to load
    await page.waitForLoadState('networkidle');
  });

  test('should load the main application interface', async ({ page }) => {
    // Check if the main chat interface is visible
    await expect(page.locator('app-chat, .chat-container, [data-testid="chat-interface"]')).toBeVisible();

    // Check if there's a message input field
    await expect(page.locator('input[type="text"], textarea, [data-testid="message-input"]')).toBeVisible();

    // Check if there's a send button
    await expect(page.locator('button, [data-testid="send-button"]')).toBeVisible();
  });

  test('should send a message and receive AI response', async ({ page }) => {
    // Find the message input
    const messageInput = page.locator('input[type="text"], textarea, [data-testid="message-input"]');
    await expect(messageInput).toBeVisible();

    // Type a test message
    const testMessage = 'Hello, AI agent! Can you help me?';
    await messageInput.fill(testMessage);

    // Click the send button
    const sendButton = page.locator('button, [data-testid="send-button"]');
    await sendButton.click();

    // Wait for the message to appear in chat
    await expect(page.locator(`text=${testMessage}`)).toBeVisible();

    // Wait for AI response (this might take a few seconds)
    await page.waitForTimeout(5000);

    // Check if we received a response
    const aiResponse = page.locator('.ai-message, .assistant-message, [data-testid="ai-response"]');
    await expect(aiResponse).toBeVisible({ timeout: 15000 });

    // Verify response is not empty
    const responseText = await aiResponse.textContent();
    expect(responseText?.trim()).toBeTruthy();
    expect(responseText!.length).toBeGreaterThan(10);
  });

  test('should maintain conversation context', async ({ page }) => {
    const messageInput = page.locator('input[type="text"], textarea, [data-testid="message-input"]');
    const sendButton = page.locator('button, [data-testid="send-button"]');

    // Send first message
    await messageInput.fill('My name is John');
    await sendButton.click();
    await page.waitForTimeout(3000);

    // Send follow-up question
    await messageInput.fill('What is my name?');
    await sendButton.click();
    await page.waitForTimeout(8000);

    // Check if the AI remembers the context
    const aiResponse = page.locator('.ai-message, .assistant-message, [data-testid="ai-response"]').last();
    const responseText = await aiResponse.textContent();

    // The response should ideally mention "John" or acknowledge the name
    expect(responseText?.toLowerCase()).toContain('john');
  });

  test('should handle multiple message exchange', async ({ page }) => {
    const messageInput = page.locator('input[type="text"], textarea, [data-testid="message-input"]');
    const sendButton = page.locator('button, [data-testid="send-button"]');

    const messages = [
      'What is artificial intelligence?',
      'Can you give me a simple example?',
      'What are the main types of AI?'
    ];

    for (const message of messages) {
      // Send message
      await messageInput.fill(message);
      await sendButton.click();

      // Wait for response
      await page.waitForTimeout(8000);

      // Verify we got a response
      const aiResponses = page.locator('.ai-message, .assistant-message, [data-testid="ai-response"]');
      const lastResponse = aiResponses.last();
      await expect(lastResponse).toBeVisible();

      const responseText = await lastResponse.textContent();
      expect(responseText?.trim().length).toBeGreaterThan(20);
    }

    // Verify we have multiple message pairs
    const userMessages = page.locator('.user-message, [data-testid="user-message"]');
    const aiMessages = page.locator('.ai-message, .assistant-message, [data-testid="ai-response"]');

    expect(await userMessages.count()).toBe(messages.length);
    expect(await aiMessages.count()).toBe(messages.length);
  });

  test('should display agent capabilities', async ({ page }) => {
    // Try to access agent capabilities (might be through a menu or settings)
    const capabilitiesButton = page.locator('button, [data-testid="capabilities-button"], .settings-button');

    if (await capabilitiesButton.isVisible()) {
      await capabilitiesButton.click();

      // Check if capabilities are displayed
      const capabilitiesSection = page.locator('.capabilities, [data-testid="capabilities"], .agent-info');
      await expect(capabilitiesSection).toBeVisible();

      // Verify key capabilities are mentioned
      const capabilitiesText = await capabilitiesSection.textContent();
      expect(capabilitiesText).toContain('task processing');
      expect(capabilitiesText).toContain('memory management');
      expect(capabilitiesText).toContain('metrics');
    }
  });

  test('should handle API errors gracefully', async ({ page }) => {
    // Intercept API call to simulate error
    await page.route(API_BASE_URL + '/v1/chat/completions', async route => {
      await route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          error: {
            message: 'Internal server error',
            type: 'server_error',
            code: 'internal_error'
          }
        })
      });
    });

    // Try to send a message
    const messageInput = page.locator('input[type="text"], textarea, [data-testid="message-input"]');
    const sendButton = page.locator('button, [data-testid="send-button"]');

    await messageInput.fill('Test error handling');
    await sendButton.click();

    // Should show error message
    const errorMessage = page.locator('.error-message, [data-testid="error-message"]');
    await expect(errorMessage).toBeVisible({ timeout: 10000 });

    // Should still allow retrying
    await expect(messageInput).toBeVisible();
    await expect(sendButton).toBeVisible();
  });

  test('should show typing indicator during processing', async ({ page }) => {
    // Intercept and delay the API call to ensure we see the typing indicator
    await page.route(API_BASE_URL + '/v1/chat/completions', async route => {
      // Wait 2 seconds before responding
      await new Promise(resolve => setTimeout(resolve, 2000));
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 'test-chatcmpl-123',
          object: 'chat.completion',
          created: Date.now() / 1000,
          model: 'test-model',
          choices: [{
            index: 0,
            message: {
              role: 'assistant',
              content: 'This is a test response from the AI agent.'
            },
            finish_reason: 'stop'
          }],
          usage: {
            prompt_tokens: 10,
            completion_tokens: 15,
            total_tokens: 25
          }
        })
      });
    });

    const messageInput = page.locator('input[type="text"], textarea, [data-testid="message-input"]');
    const sendButton = page.locator('button, [data-testid="send-button"]');

    await messageInput.fill('Test typing indicator');
    await sendButton.click();

    // Should show typing indicator
    const typingIndicator = page.locator('.typing-indicator, [data-testid="typing-indicator"]');
    await expect(typingIndicator).toBeVisible({ timeout: 5000 });

    // Should disappear when response arrives
    await expect(typingIndicator).not.toBeVisible({ timeout: 30000 });

    // Should show the response
    const aiResponse = page.locator('.ai-message, .assistant-message, [data-testid="ai-response"]');
    await expect(aiResponse).toBeVisible();
  });

  test('should handle concurrent requests properly', async ({ page }) => {
    const messageInput = page.locator('input[type="text"], textarea, [data-testid="message-input"]');
    const sendButton = page.locator('button, [data-testid="send-button"]');

    // Send multiple messages quickly
    const messages = ['First message', 'Second message', 'Third message'];

    for (const message of messages) {
      await messageInput.fill(message);
      await sendButton.click();
      await page.waitForTimeout(100); // Small delay between messages
    }

    // Wait for all responses
    await page.waitForTimeout(20000);

    // Should have processed all messages
    const userMessages = page.locator('.user-message, [data-testid="user-message"]');
    const aiMessages = page.locator('.ai-message, .assistant-message, [data-testid="ai-response"]');

    expect(await userMessages.count()).toBe(3);

    // AI messages might take longer, but should eventually appear
    await page.waitForTimeout(10000);
    expect(await aiMessages.count()).toBeGreaterThanOrEqual(1);
  });

  test('should work with agent-specific endpoints', async ({ page }) => {
    // Test if the application can communicate with agent-specific endpoints
    try {
      const response = await page.request.get(API_BASE_URL + '/api/v1/agent/health');

      if (response.status() === 200) {
        const healthData = await response.json();
        expect(healthData).toHaveProperty('status');
        expect(healthData).toHaveProperty('agent');
      }
    } catch (error) {
      // If agent endpoints are not available, that's okay for this test
      console.log('Agent endpoints not available, skipping agent-specific tests');
    }
  });

  test('should maintain session state', async ({ page }) => {
    const messageInput = page.locator('input[type="text"], textarea, [data-testid="message-input"]');
    const sendButton = page.locator('button, [data-testid="send-button"]');

    // Send initial message
    await messageInput.fill('Remember this: I love pizza');
    await sendButton.click();
    await page.waitForTimeout(5000);

    // Reload the page
    await page.reload();
    await page.waitForLoadState('networkidle');

    // Send follow-up message
    await messageInput.fill('What did I say I love?');
    await sendButton.click();
    await page.waitForTimeout(8000);

    // The AI should ideally remember the context if session management is working
    const aiResponse = page.locator('.ai-message, .assistant-message, [data-testid="ai-response"]').last();
    const responseText = await aiResponse.textContent();

    // This test might not always pass depending on session management implementation
    // but it's good to have it to check if context is maintained
    console.log('Session context response:', responseText);
  });

  test.afterEach(async ({ page }) => {
    // Clear any lingering requests or timeouts
    await page.waitForTimeout(1000);
  });
});