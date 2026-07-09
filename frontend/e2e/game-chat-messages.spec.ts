import {expect, test} from '@playwright/test';

import {
  expectChatMessageVisible,
  expectChatReady,
  roomCodeFromUrl,
  sendChatMessage,
  startGameWith2Players,
  uniquePlayerName,
} from './room-flow-helpers';

test('lets players send and receive chat messages during an active game', async ({
  browser,
  page: hostPage,
}) => {
  const guestContext = await browser.newContext();
  const guestPage = await guestContext.newPage();
  const hostName = uniquePlayerName('Host');
  const guestName = uniquePlayerName('Guest');
  const messageSuffix = Date.now().toString(36);
  const hostMessage = `In-game hello from host ${messageSuffix}`;
  const guestMessage = `In-game hello from guest ${messageSuffix}`;

  try {
    const roomCode = await startGameWith2Players(hostPage, hostName, guestPage, guestName);
    expect(roomCodeFromUrl(hostPage)).toBe(roomCode);
    expect(roomCodeFromUrl(guestPage)).toBe(roomCode);

    await expectChatReady(hostPage);
    await expectChatReady(guestPage);

    await sendChatMessage(hostPage, hostMessage);
    await expectChatMessageVisible(hostPage, hostName, hostMessage);
    await expectChatMessageVisible(guestPage, hostName, hostMessage);

    await sendChatMessage(guestPage, guestMessage);
    await expectChatMessageVisible(guestPage, guestName, guestMessage);
    await expectChatMessageVisible(hostPage, guestName, guestMessage);
  } finally {
    await guestContext.close();
  }
});
