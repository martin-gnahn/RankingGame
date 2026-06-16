import { expect, test } from '@playwright/test';

test('loads the home screen', async ({ page }) => {
  await page.goto('/');

  await expect(page.getByRole('heading', { name: 'Ranking Game' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Raum erstellen' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Raum beitreten' })).toBeVisible();
});
