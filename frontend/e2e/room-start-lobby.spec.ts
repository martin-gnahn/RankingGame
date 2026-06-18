import { expect, test } from '@playwright/test';

import { createRoom, joinRoom, uniquePlayerName } from './room-flow-helpers';

test('allows the host to start from the lobby while non-host players cannot', async ({
  browser,
  page: hostPage,
}) => {
  const guestContext = await browser.newContext();
  const guestPage = await guestContext.newPage();
  const hostName = uniquePlayerName('Host');
  const guestName = uniquePlayerName('Guest');

  try {
    await hostPage.goto('/');
    const roomCode = await createRoom(hostPage, hostName);

    await guestPage.goto('/');
    await joinRoom(guestPage, roomCode, guestName);

    await expect(hostPage.getByRole('button', { name: 'Spiel starten' })).toBeVisible();
    await expect(hostPage.getByRole('button', { name: 'Spiel starten' })).toBeEnabled();
    await expect(guestPage.getByRole('button', { name: 'Spiel starten' })).toHaveCount(0);
    await expect(guestPage.getByText('Der Host startet das Spiel.')).toBeVisible();

    await hostPage.getByRole('button', { name: 'Spiel starten' }).click();

    await expect(hostPage.getByText('Spiel laeuft').first()).toBeVisible();
    await expect(guestPage.getByText('Spiel laeuft').first()).toBeVisible();
  } finally {
    await guestContext.close();
  }
});
