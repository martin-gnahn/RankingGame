import {expect, Locator, Page} from '@playwright/test';

import {t} from './i18n';

const backendHealthUrl = process.env.E2E_BACKEND_HEALTH_URL ?? 'http://localhost:8080/health';
let backendReady = false;
const connectionStatusTranslationKeys = {
  disconnected: 'lobby.connection.disconnected',
  online: 'lobby.connection.online',
} as const;

export function uniquePlayerName(prefix: string): string {
  return `${prefix} ${Date.now().toString(36).slice(-6)}`;
}

export async function createRoom(page: Page, playerName: string): Promise<string> {
  await ensureBackendReady(page);

  await page.locator('app-create-room').getByLabel(t('createRoom.playerName')).fill(playerName);
  await page.locator('app-create-room').getByRole('button', {name: t('createRoom.submit')}).click();
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

  await joinForm.getByLabel(t('joinRoom.roomCode')).fill(roomCode);
  await joinForm.getByLabel(t('joinRoom.playerName')).fill(playerName);
  await joinForm.getByRole('button', {name: t('joinRoom.submit')}).click();
}

export async function expectPlayerVisible(page: Page, playerName: string): Promise<void> {
  await expect(page.getByRole('heading', { name: roomCodeFromUrl(page) })).toBeVisible();
  await expect(page.locator('.player-list')).toContainText(playerName);
}

export async function expectPlayerConnectionStatus(
  page: Page,
  playerName: string,
  status: keyof typeof connectionStatusTranslationKeys,
): Promise<void> {
  const statusLabel = t(connectionStatusTranslationKeys[status]);

  await expect(
    page.locator('.player-row').filter({ hasText: playerName }).getByText(statusLabel),
  ).toBeVisible();
}

export async function expectStartGameButtonState(
  page: Page,
  expectedState: 'enabled' | 'disabled',
): Promise<void> {
  const startButton = startGameButton(page);

  if (expectedState === 'enabled') {
    await expect(startButton).toBeEnabled();
    return;
  }

  await expect(startButton).toBeDisabled();
}

export async function startGame(page: Page): Promise<void> {
  await expectStartGameButtonState(page, 'enabled');
  await startGameButton(page).click();
}

export async function expectGameScreen(page: Page): Promise<void> {
  await page.waitForURL(/\/game\/[A-Z0-9]{4,8}(?:\?.*)?$/);
  let roundTranslated = t('game.roundLabel', {roundNumber: 1});
  debugger;
  await expect(page.getByText(roundTranslated)).toBeVisible();
  await expect(page.getByRole('textbox', {name: t('game.answer.label')})).toBeVisible();
  await expect(page.getByRole('button', {name: t('game.submit.default')})).toBeVisible();
}

export async function submitAnswer(page: Page, answerText: string): Promise<void> {
  await page.getByRole('textbox', {name: t('game.answer.label')}).fill(answerText);
  await page.getByRole('button', {name: t('game.submit.default')}).click();
  await expect(page.getByRole('button', {name: t('game.submit.sent')})).toBeVisible();
}

export async function expectChatReady(page: Page): Promise<void> {
  const chat = page.locator('app-chat-sidebar');

  await expect(chat.getByRole('heading', {name: t('chat.title')})).toBeVisible();
  await expect(chat.getByRole('textbox', {name: t('chat.messageLabel')})).toBeVisible();
  await expect(chat.getByRole('button', {name: t('chat.send')})).toBeEnabled();
}

export async function sendChatMessage(page: Page, messageBody: string): Promise<void> {
  const chat = page.locator('app-chat-sidebar');

  await chat.getByRole('textbox', {name: t('chat.messageLabel')}).fill(messageBody);
  await chat.getByRole('button', {name: t('chat.send')}).click();
  await expect(chat.getByRole('textbox', {name: t('chat.messageLabel')})).toHaveValue('');
}

export async function expectChatMessageVisible(
  page: Page,
  senderNickname: string,
  messageBody: string,
): Promise<void> {
  const message = page
    .getByRole('log')
    .locator('.message')
    .filter({ hasText: messageBody });

  await expect(message).toContainText(senderNickname);
  await expect(message).toContainText(messageBody);
}

export function roomCodeFromUrl(page: Page): string {
  const roomCode = new URL(page.url()).pathname.match(/\/(?:lobby|game)\/([A-Z0-9]{4,8})$/)?.[1];

  if (!roomCode) {
    throw new Error(`Could not read room code from URL: ${page.url()}`);
  }

  return roomCode;
}

function startGameButton(page: Page): Locator {
  return page.getByRole('button', {name: t('lobby.startGame')});
}

async function ensureBackendReady(page: Page): Promise<void> {
  if (backendReady) {
    return;
  }

  let response;
  try {
    response = await page.request.get(backendHealthUrl, { timeout: 5_000 });
  } catch (error) {
    throw new Error(
      `RankingGame backend is not reachable at ${backendHealthUrl}. ` +
        'Start PostgreSQL with `docker compose up -d` and the backend with `./mvnw spring-boot:run` from the repository root. ' +
        `Original error: ${String(error)}`,
    );
  }

  if (!response.ok()) {
    throw new Error(
      `RankingGame backend health check failed at ${backendHealthUrl}: ${response.status()} ${response.statusText()}`,
    );
  }

  backendReady = true;
}

export async function startGameWith2Players(hostPage: Page, hostName: string, guestPage: Page, guestName: string) {
  await hostPage.goto('/');
  const roomCode = await createRoom(hostPage, hostName);

  await guestPage.goto('/');
  await joinRoom(guestPage, roomCode, guestName);

  await startGame(hostPage);

  await expectGameScreen(hostPage);
  await expectGameScreen(guestPage);
  return roomCode;
}
