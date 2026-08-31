import {expect, Page, test} from '@playwright/test';

import {
  createRoomWith2Players,
  expectChatMessageVisible,
  expectChatReady,
  roomCodeFromUrl,
  sendChatMessage,
  startGameWith2Players,
  uniquePlayerName,
} from './room-flow-helpers';

type ChatExpectationParams = {
  hostPage: Page;
  guestPage: Page;
  hostMessage: string;
  hostName: string;
  guestMessage: string;
  guestName: string;
};

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

    await expectChatToBeWorkingAsExpected({
      hostPage,
      guestPage,
      hostMessage,
      hostName,
      guestMessage,
      guestName,
    });
  } finally {
    await guestContext.close();
  }
});

test('lets players send and receive chat messages in lobby', async ({
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
    const roomCode = await createRoomWith2Players(hostPage, hostName, guestPage, guestName);
    expect(roomCodeFromUrl(hostPage)).toBe(roomCode);
    expect(roomCodeFromUrl(guestPage)).toBe(roomCode);

    await expectChatToBeWorkingAsExpected({
      hostPage,
      guestPage,
      hostMessage,
      hostName,
      guestMessage,
      guestName,
    });
  } finally {
    await guestContext.close();
  }
});

async function expectChatToBeWorkingAsExpected({
                                                 hostPage,
                                                 guestPage,
                                                 hostMessage,
                                                 hostName,
                                                 guestMessage,
                                                 guestName,
                                               }: ChatExpectationParams) {
  await expectChatReady(hostPage);
  await expectChatReady(guestPage);

  await sendChatMessage(hostPage, hostMessage);
  await expectChatMessageVisible(hostPage, hostName, hostMessage);
  await expectChatMessageVisible(guestPage, hostName, hostMessage);

  await sendChatMessage(guestPage, guestMessage);
  await expectChatMessageVisible(guestPage, guestName, guestMessage);
  await expectChatMessageVisible(hostPage, guestName, guestMessage);
}
