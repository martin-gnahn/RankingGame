import { defineAppPlaywrightConfig } from './playwright.shared';

const baseURL = requiredEnv('PLAYWRIGHT_PUBLIC_BASE_URL');

process.env.E2E_BACKEND_HEALTH_URL ??= requiredEnv('E2E_PUBLIC_BACKEND_HEALTH_URL');

export default defineAppPlaywrightConfig({
  baseURL,
});

function requiredEnv(name: string): string {
  const value = process.env[name];

  if (!value) {
    throw new Error(`${name} must be set when using playwright.public.config.ts.`);
  }

  return value;
}
