import { test } from '@playwright/test';

import {
  createRoom,
  expectGameScreen,
  joinRoom,
  startGame,
  submitAnswer,
  uniquePlayerName,
} from './room-flow-helpers';

test('lets two players submit answers after the host starts the game', async ({
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

    await startGame(hostPage);

    await expectGameScreen(hostPage);
    await expectGameScreen(guestPage);

    await submitAnswer(hostPage, 'Ich wuerde erst mal die Kaffeemaschine beschuldigen.');
    await submitAnswer(guestPage, 'Ich sage, das WLAN hatte Lampenfieber.');
  } finally {
    await guestContext.close();
  }
});
