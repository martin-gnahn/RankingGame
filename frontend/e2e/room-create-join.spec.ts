import { expect, test } from '@playwright/test';

import { createRoom, expectPlayerVisible, joinRoom, uniquePlayerName } from './room-flow-helpers';

test('shows a joined player in the host lobby', async ({ browser, page: hostPage }) => {
  const guestContext = await browser.newContext();
  const guestPage = await guestContext.newPage();
  const hostName = uniquePlayerName('Host');
  const guestName = uniquePlayerName('Guest');

  try {
    await hostPage.goto('/');

    const roomCode = await createRoom(hostPage, hostName);

    await guestPage.goto('/');
    await joinRoom(guestPage, roomCode, guestName);

    await hostPage.reload();
    await expectPlayerVisible(hostPage, guestName);
  } finally {
    await guestContext.close();
  }
});
