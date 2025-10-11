Playwright E2E Tests

- Location: `ui/e2e`
- Config: `ui/playwright.config.ts`

Run locally

- Start server automatically: from `ui/`, run `npm run test:e2e`
- Or reuse a running dev server: start `ng serve` in `ui/`, then run:
  - Windows PowerShell: `$env:E2E_SKIP_SERVER='1'; npm run test:e2e`
  - Bash: `E2E_SKIP_SERVER=1 npm run test:e2e`

Headed mode

- `npm run test:e2e:headed`

Artifacts

- Results: `ui/test-results`
- HTML report: `playwright-report` (open with `npx playwright show-report` from `ui/`)

Notes

- Tests mock API calls to `/v1/threads` and `/v1/chat/completions` for reliability.
- Streaming is toggled off in tests to exercise the JSON response path.
