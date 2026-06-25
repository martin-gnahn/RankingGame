import { defineConfig, devices, type PlaywrightTestConfig } from '@playwright/test';

type AppPlaywrightConfigOptions = {
  baseURL: string;
  webServer?: PlaywrightTestConfig['webServer'];
};

export function defineAppPlaywrightConfig({ baseURL, webServer }: AppPlaywrightConfigOptions) {
  return defineConfig({
    testDir: './e2e',
    outputDir: './test-results',
    fullyParallel: true,
    forbidOnly: !!process.env.CI,
    retries: process.env.CI ? 2 : 0,
    workers: process.env.CI ? 1 : undefined,
    reporter: [['list'], ['html', { open: 'never' }]],
    timeout: 30_000,
    expect: {
      timeout: 10_000,
    },
    use: {
      baseURL,
      screenshot: 'only-on-failure',
      trace: 'retain-on-failure',
      video: 'retain-on-failure',
      launchOptions: {
        args: ['--auto-open-devtools-for-tabs'],
      },
    },
    projects: [
      {
        name: 'chromium',
        use: { ...devices['Desktop Chrome'] },
      },
      {
        name: 'firefox',
        use: { ...devices['Desktop Firefox'] },
      },
      {
        name: 'webkit',
        use: { ...devices['Desktop Safari'] },
      },
    ],
    webServer,
  });
}
