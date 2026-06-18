import { expect, test } from '@playwright/test';

import {
  createRoom,
  expectGameScreen,
  expectPlayerConnectionStatus,
  joinRoom,
  startGame,
  uniquePlayerName,
} from './room-flow-helpers';

test('does not allow the host to start when only the host is in the lobby', async ({
  page: hostPage,
}) => {
  const hostName = uniquePlayerName('Host');

  await hostPage.goto('/');
  await createRoom(hostPage, hostName);

  await expect(hostPage.getByText('1 Spieler')).toBeVisible();
  await expect(hostPage.getByRole('button', { name: 'Spiel starten' })).toBeDisabled();
  await expect(hostPage).toHaveURL(/\/lobby\/[A-Z0-9]{4,8}(?:\?.*)?$/);
});

test('does not allow start when every non-host player is disconnected', async ({
  browser,
  page: hostPage,
}) => {
  const guestContext = await browser.newContext();
  const guestPage = await guestContext.newPage();
  const hostName = uniquePlayerName('Host');
  const guestName = uniquePlayerName('Guest');
  let guestContextClosed = false;

  await hostPage.goto('/');
  const roomCode = await createRoom(hostPage, hostName);

  try {
    await guestPage.goto('/');
    await joinRoom(guestPage, roomCode, guestName);

    await expectPlayerConnectionStatus(hostPage, guestName, 'Online');
    await guestContext.close();
    guestContextClosed = true;
    await expectPlayerConnectionStatus(hostPage, guestName, 'Getrennt');

    await expect(hostPage.getByRole('button', { name: 'Spiel starten' })).toBeDisabled();
    await expect(hostPage).toHaveURL(new RegExp(`/lobby/${roomCode}(?:\\?.*)?$`));
  } finally {
    if (!guestContextClosed) {
      await guestContext.close();
    }
  }
});

test('allows the host to start when another player is online while non-host players cannot', async ({
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

    await expectPlayerConnectionStatus(hostPage, guestName, 'Online');
    await expect(hostPage.getByRole('button', { name: 'Spiel starten' })).toBeVisible();
    await expect(hostPage.getByRole('button', { name: 'Spiel starten' })).toBeEnabled();
    await expect(guestPage.getByRole('button', { name: 'Spiel starten' })).toHaveCount(0);
    await expect(guestPage.getByText('Der Host startet das Spiel.')).toBeVisible();

    await startGame(hostPage);

    await expectGameScreen(hostPage);
    await expectGameScreen(guestPage);
  } finally {
    await guestContext.close();
  }
});
