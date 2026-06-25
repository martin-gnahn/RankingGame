import { defineAppPlaywrightConfig } from './playwright.shared';

const baseURL = process.env.PLAYWRIGHT_BASE_URL ?? 'http://localhost:4200';
const startWebServer = process.env.PLAYWRIGHT_SKIP_WEB_SERVER !== '1';

export default defineAppPlaywrightConfig({
  baseURL,
  webServer: startWebServer
    ? {
        command: 'npm run start -- --host localhost --port 4200',
        url: baseURL,
        reuseExistingServer: !process.env.CI,
        timeout: 120_000,
        stdout: 'pipe',
        stderr: 'pipe',
      }
    : undefined,
});
