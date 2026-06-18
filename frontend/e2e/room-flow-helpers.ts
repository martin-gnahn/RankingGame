import { Page, expect } from '@playwright/test';

export function uniquePlayerName(prefix: string): string {
  return `${prefix} ${Date.now().toString(36).slice(-6)}`;
}

export async function createRoom(page: Page, playerName: string): Promise<string> {
  await page.locator('app-create-room').getByLabel('Dein Name').fill(playerName);
  await page.locator('app-create-room').getByRole('button', { name: 'Raum erstellen' }).click();
  await page.waitForURL(/\/lobby\/[A-Z0-9]{4,8}(?:\?.*)?$/);

  return roomCodeFromUrl(page);
}

export async function joinRoom(page: Page, roomCode: string, playerName: string): Promise<void> {
  await submitJoinRoom(page, roomCode, playerName);
  await page.waitForURL(new RegExp(`/lobby/${roomCode}(?:\\?.*)?$`));
}

export async function submitJoinRoom(
  page: Page,
  roomCode: string,
  playerName: string,
): Promise<void> {
  const joinForm = page.locator('app-join-room');

  await joinForm.getByLabel('Raumcode').fill(roomCode);
  await joinForm.getByLabel('Dein Name').fill(playerName);
  await joinForm.getByRole('button', { name: 'Raum beitreten' }).click();
}

export async function expectPlayerVisible(page: Page, playerName: string): Promise<void> {
  await expect(page.getByRole('heading', { name: roomCodeFromUrl(page) })).toBeVisible();
  await expect(page.locator('.player-list')).toContainText(playerName);
}

export function roomCodeFromUrl(page: Page): string {
  const roomCode = new URL(page.url()).pathname.match(/\/lobby\/([A-Z0-9]{4,8})$/)?.[1];

  if (!roomCode) {
    throw new Error(`Could not read room code from URL: ${page.url()}`);
  }

  return roomCode;
}
