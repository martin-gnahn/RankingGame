import {expect, Locator, Page} from '@playwright/test';

const backendHealthUrl = process.env.E2E_BACKEND_HEALTH_URL ?? 'http://localhost:8080/health';
let backendReady = false;

export function uniquePlayerName(prefix: string): string {
  return `${prefix} ${Date.now().toString(36).slice(-6)}`;
}

export async function createRoom(page: Page, playerName: string): Promise<string> {
  await ensureBackendReady(page);

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

export async function expectPlayerConnectionStatus(
  page: Page,
  playerName: string,
  statusLabel: 'Online' | 'Getrennt',
): Promise<void> {
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
  await expect(page.getByText(/^Runde \d+$/)).toBeVisible();
  await expect(page.getByRole('textbox', { name: 'Antwort' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Antwort abgeben' })).toBeVisible();
}

export async function submitAnswer(page: Page, answerText: string): Promise<void> {
  await page.getByRole('textbox', { name: 'Antwort' }).fill(answerText);
  await page.getByRole('button', { name: 'Antwort abgeben' }).click();
  await expect(page.getByRole('button', { name: 'Antwort gesendet' })).toBeVisible();
  await expect(page.getByText('Antwort gespeichert. Warte auf die anderen Spieler.')).toBeVisible();
}

export async function expectChatReady(page: Page): Promise<void> {
  const chat = page.locator('app-chat-sidebar');

  await expect(chat.getByRole('heading', { name: 'Chat' })).toBeVisible();
  await expect(chat.getByRole('textbox', { name: 'Nachricht' })).toBeVisible();
  await expect(chat.getByRole('button', { name: 'Senden' })).toBeEnabled();
}

export async function sendChatMessage(page: Page, messageBody: string): Promise<void> {
  const chat = page.locator('app-chat-sidebar');

  await chat.getByRole('textbox', { name: 'Nachricht' }).fill(messageBody);
  await chat.getByRole('button', { name: 'Senden' }).click();
  await expect(chat.getByRole('textbox', { name: 'Nachricht' })).toHaveValue('');
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
  return page.getByRole('button', {name: 'Spiel starten'});
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
