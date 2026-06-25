import { expect, test } from '@playwright/test';

import {
  createRoom,
  expectGameScreen,
  expectPlayerConnectionStatus,
  joinRoom,
  startGame,
  uniquePlayerName,
} from './room-flow-helpers';

test('shows an error when the host starts with no other players in the lobby', async ({
  page: hostPage,
}) => {
  const hostName = uniquePlayerName('Host');

  await hostPage.goto('/');
  await createRoom(hostPage, hostName);

  await expect(hostPage.getByText('1 Spieler')).toBeVisible();
  await expect(hostPage.getByRole('button', { name: 'Spiel starten' })).toBeEnabled();
  await hostPage.getByRole('button', { name: 'Spiel starten' }).click();
  await expect(hostPage.getByRole('alert')).toContainText(
    'At least 2 players are required to start the game',
  );
  await expect(hostPage).toHaveURL(/\/lobby\/[A-Z0-9]{4,8}(?:\?.*)?$/);
});

test('shows an error when every non-host player is disconnected', async ({
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

    await expect(hostPage.getByRole('button', { name: 'Spiel starten' })).toBeEnabled();
    await hostPage.getByRole('button', { name: 'Spiel starten' }).click();
    await expect(hostPage.getByRole('alert')).toContainText(
      'At least 2 players are required to start the game',
    );
    await expect(hostPage).toHaveURL(new RegExp(`/lobby/${roomCode}(?:\\?.*)?$`));
  } finally {
    if (!guestContextClosed) {
      await guestContext.close();
    }
  }
});

test('allows the host to start when one joined player remains online', async ({
  browser,
  page: hostPage,
}) => {
  const guestOneContext = await browser.newContext();
  const guestTwoContext = await browser.newContext();
  const guestOnePage = await guestOneContext.newPage();
  const guestTwoPage = await guestTwoContext.newPage();
  const hostName = uniquePlayerName('Player1');
  const guestOneName = uniquePlayerName('Player2');
  const guestTwoName = uniquePlayerName('Player3');
  let guestOneContextClosed = false;
  let guestTwoContextClosed = false;

  await hostPage.goto('/');
  const roomCode = await createRoom(hostPage, hostName);

  try {
    await guestOnePage.goto('/');
    await joinRoom(guestOnePage, roomCode, guestOneName);
    await guestTwoPage.goto('/');
    await joinRoom(guestTwoPage, roomCode, guestTwoName);

    await expectPlayerConnectionStatus(hostPage, guestOneName, 'Online');
    await expectPlayerConnectionStatus(hostPage, guestTwoName, 'Online');

    await guestOneContext.close();
    guestOneContextClosed = true;

    await expectPlayerConnectionStatus(hostPage, guestOneName, 'Getrennt');
    await expectPlayerConnectionStatus(hostPage, guestTwoName, 'Online');

    await expect(hostPage.getByRole('button', { name: 'Spiel starten' })).toBeEnabled();
    await startGame(hostPage);

    await expectGameScreen(hostPage);
    await expectGameScreen(guestTwoPage);
  } finally {
    if (!guestOneContextClosed) {
      await guestOneContext.close();
    }

    if (!guestTwoContextClosed) {
      await guestTwoContext.close();
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
