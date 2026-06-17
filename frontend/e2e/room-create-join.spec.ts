import { Page, expect, test } from '@playwright/test';

test('shows a joined player in the host lobby', async ({ browser }) => {
  const suffix = Date.now().toString(36).slice(-6);
  const hostName = `Host ${suffix}`;
  const guestName = `Guest ${suffix}`;

  const hostContext = await browser.newContext();
  const guestContext = await browser.newContext();

  const hostPage = await hostContext.newPage();
  const guestPage = await guestContext.newPage();

  try {
    await hostPage.goto('/');

    const roomCode = await createRoom(hostPage, hostName);

    await guestPage.goto('/');
    await joinRoom(guestPage, roomCode, guestName);

    // await hostPage.pause(); // reicht meistens

    await hostPage.reload();
    await expectPlayerVisible(hostPage, guestName);
  } finally {
    // these cause this error
    await guestContext.close();
    await hostContext.close();
  }
});

async function createRoom(page: Page, playerName: string): Promise<string> {
  await page.locator('app-create-room').getByLabel('Dein Name').fill(playerName);
  await page.locator('app-create-room').getByRole('button', { name: 'Raum erstellen' }).click();
  await page.waitForURL(/\/lobby\/[A-Z0-9]{4,8}(?:\?.*)?$/);

  return roomCodeFromUrl(page);
}

async function joinRoom(page: Page, roomCode: string, playerName: string): Promise<void> {
  const joinForm = page.locator('app-join-room');

  await joinForm.getByLabel('Raumcode').fill(roomCode);
  await joinForm.getByLabel('Dein Name').fill(playerName);
  await joinForm.getByRole('button', { name: 'Raum beitreten' }).click();
  await page.waitForURL(new RegExp(`/lobby/${roomCode}(?:\\?.*)?$`));
}

async function expectPlayerVisible(page: Page, playerName: string): Promise<void> {
  await expect(page.getByRole('heading', { name: roomCodeFromUrl(page) })).toBeVisible();
  await expect(page.locator('.player-list')).toContainText(playerName);
}

function roomCodeFromUrl(page: Page): string {
  const roomCode = new URL(page.url()).pathname.match(/\/lobby\/([A-Z0-9]{4,8})$/)?.[1];

  if (!roomCode) {
    throw new Error(`Could not read room code from URL: ${page.url()}`);
  }

  return roomCode;
}
