import {expect, test} from '@playwright/test';

import {t} from './i18n';

test('loads the home screen', async ({ page }) => {
  await page.goto('/');

  await expect(page.getByRole('heading', {name: t('home.title')})).toBeVisible();
  await expect(page.getByRole('button', {name: t('createRoom.submit')})).toBeVisible();
  await expect(page.getByRole('button', {name: t('joinRoom.submit')})).toBeVisible();
});
