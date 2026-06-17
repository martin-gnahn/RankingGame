import { expect, test } from '@playwright/test';

import { createRoom, joinRoom, submitJoinRoom, uniquePlayerName } from './room-flow-helpers';

test('rejects duplicate nicknames ignoring case', async ({ browser, page: hostPage }) => {
  const firstGuestContext = await browser.newContext();
  const duplicateGuestContext = await browser.newContext();
  const firstGuestPage = await firstGuestContext.newPage();
  const duplicateGuestPage = await duplicateGuestContext.newPage();
  const hostName = uniquePlayerName('Host');
  const guestName = uniquePlayerName('Casey');
  const duplicateGuestName = guestName.toUpperCase();

  try {
    await hostPage.goto('/');
    const roomCode = await createRoom(hostPage, hostName);

    await firstGuestPage.goto('/');
    await joinRoom(firstGuestPage, roomCode, guestName);

    await duplicateGuestPage.goto('/');
    await submitJoinRoom(duplicateGuestPage, roomCode.toLowerCase(), duplicateGuestName);

    await expect(duplicateGuestPage.getByRole('alert')).toContainText(
      'Player name is already taken',
    );
    await expect(duplicateGuestPage).toHaveURL(/\/$/);
  } finally {
    await duplicateGuestContext.close();
    await firstGuestContext.close();
  }
});
