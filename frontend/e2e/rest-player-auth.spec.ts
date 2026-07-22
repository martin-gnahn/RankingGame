import {expect, type Page, type Request, test} from '@playwright/test';

import {createRoom, expectGameScreen, joinRoom, startGame, submitAnswer, uniquePlayerName,} from './room-flow-helpers';
import {t} from './i18n';

type StoredPlayerData = {
  playerId: string;
  role: 'host' | 'player';
  playerSessionToken: string | null;
};
type ErrorPageExpectation = {
  title: string;
  message: string;
  status: string;
  detailMessage: string;
};

const PLAYER_DATA_STORAGE_KEY = 'playerData';
const PLAYER_SESSION_TOKEN_HEADER = 'X-Player-Session-Token';
const NO_TOKEN_MESSAGE = 'No user authentication token.';
const INVALID_TOKEN_MESSAGE = 'User is not authorized to access backend.';
const ANSWER_TEXT = 'Ich wuerde erst mal die Kaffeemaschine beschuldigen.';
const NO_TOKEN_ERROR_PAGE: ErrorPageExpectation = {
  title: t('error.noToken.title'),
  message: t('error.noToken.message'),
  status: '401',
  detailMessage: NO_TOKEN_MESSAGE,
};
const INVALID_TOKEN_ERROR_PAGE: ErrorPageExpectation = {
  title: t('error.invalidToken.title'),
  message: t('error.invalidToken.message'),
  status: '401',
  detailMessage: INVALID_TOKEN_MESSAGE,
};

test('proves valid REST auth with session token and no playerId in protected URLs or bodies', async ({
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
    await expectStoredTokenToExist(hostPage);

    await guestPage.goto('/');
    await joinRoom(guestPage, roomCode, guestName);
    await expectStoredTokenToExist(guestPage);

    await reloadAndAssertProtectedGetUsesSessionTokenOnly(hostPage, roomCode);
    await reloadAndAssertProtectedGetUsesSessionTokenOnly(guestPage, roomCode);

    const startGameRequestPromise = waitForStartGameRequest(hostPage, roomCode);
    await startGame(hostPage);
    assertStartGameRequestUsesSessionTokenOnly(await startGameRequestPromise);

    await expectGameScreen(hostPage);
    await expectGameScreen(guestPage);

    await reloadAndAssertProtectedGetUsesSessionTokenOnly(hostPage, roomCode, '/ranking-game/current-round');
    await reloadAndAssertProtectedGetUsesSessionTokenOnly(guestPage, roomCode, '/ranking-game/current-round');

    const submitAnswerRequestPromise = waitForSubmitAnswerRequest(hostPage, roomCode);
    await submitAnswer(hostPage, ANSWER_TEXT);
    assertSubmitAnswerRequestUsesSessionTokenOnly(await submitAnswerRequestPromise);
  } finally {
    await guestContext.close();
  }
});

test.describe('REST auth rejection', () => {
  test('redirects a fresh browser context without local session to the no-token error page', async ({
                                                                                                      browser,
                                                                                                      page: hostPage,
                                                                                                    }) => {
    await hostPage.goto('/');
    const roomCode = await createRoom(hostPage, uniquePlayerName('Host'));

    const freshContext = await browser.newContext();
    const freshPage = await freshContext.newPage();

    try {
      const protectedRoomRequests = collectProtectedRoomRequests(freshPage, roomCode);

      await freshPage.goto(`/lobby/${roomCode}`);

      await expectErrorPage(freshPage, NO_TOKEN_ERROR_PAGE);
      expect(protectedRoomRequests).toEqual([]);
    } finally {
      await freshContext.close();
    }
  });

  test('redirects to the invalid-token error page when a stored token is tampered with', async ({
                                                                                                  page,
                                                                                                }) => {
    await page.goto('/');
    const roomCode = await createRoom(page, uniquePlayerName('Host'));

    await overwriteStoredToken(page, 'tampered-token');

    await triggerUnauthorizedRoomRequestAndExpectErrorPage(page, roomCode, () => page.reload());
    expect(await readStoredPlayerData(page)).toBeNull();
  });

  test('rejects a token from room A when it is reused against room B', async ({
                                                                                browser,
                                                                                page: roomAPage,
                                                                              }) => {
    const roomBContext = await browser.newContext();
    const roomBPage = await roomBContext.newPage();

    try {
      await roomAPage.goto('/');
      await createRoom(roomAPage, uniquePlayerName('RoomA Host'));
      const roomAToken = (await readStoredPlayerData(roomAPage))?.playerSessionToken;
      expect(roomAToken).toBeTruthy();

      await roomBPage.goto('/');
      const roomBCode = await createRoom(roomBPage, uniquePlayerName('RoomB Host'));
      await overwriteStoredToken(roomBPage, roomAToken!);

      await triggerUnauthorizedRoomRequestAndExpectErrorPage(roomBPage, roomBCode, () =>
        roomBPage.goto(`/lobby/${roomBCode}`),
      );
      expect(await readStoredPlayerData(roomBPage)).toBeNull();
    } finally {
      await roomBContext.close();
    }
  });
});

async function reloadAndAssertProtectedGetUsesSessionTokenOnly(
  page: Page,
  roomCode: string,
  suffix = '',
): Promise<void> {
  const requestPromise = waitForProtectedRoomGetRequest(page, roomCode, suffix);
  await page.reload();
  assertProtectedGetRequestUsesSessionTokenOnly(await requestPromise, roomCode, suffix);
}

function collectProtectedRoomRequests(page: Page, roomCode: string): string[] {
  const protectedRoomRequests: string[] = [];

  page.on('request', (request) => {
    if (request.url().includes(`/api/rooms/${roomCode}`)) {
      protectedRoomRequests.push(request.url());
    }
  });

  return protectedRoomRequests;
}

async function triggerUnauthorizedRoomRequestAndExpectErrorPage(
  page: Page,
  roomCode: string,
  triggerRequest: () => Promise<unknown>,
): Promise<void> {
  const unauthorizedResponsePromise = page.waitForResponse((response) =>
    response.url().includes(`/api/rooms/${roomCode}`)
    && response.status() === 401
  );

  await triggerRequest();
  await unauthorizedResponsePromise;
  await expectErrorPage(page, INVALID_TOKEN_ERROR_PAGE);
}

function waitForProtectedRoomGetRequest(page: Page, roomCode: string, suffix: string) {
  return page.waitForRequest((request) =>
    request.method() === 'GET'
    && request.url().includes(`/api/rooms/${roomCode}${suffix}`)
  );
}

function waitForStartGameRequest(page: Page, roomCode: string) {
  return page.waitForRequest((request) =>
    request.method() === 'POST'
    && request.url().includes(`/api/rooms/${roomCode}/ranking-game/start`)
  );
}

function assertStartGameRequestUsesSessionTokenOnly(startGameRequest: Request): void {
  expect(startGameRequest.headerValue(PLAYER_SESSION_TOKEN_HEADER)).toBeTruthy();
  expect(startGameRequest.postDataJSON()).toEqual({});
  expect(startGameRequest.postData() ?? '').not.toContain('playerId');
  expect(startGameRequest.url()).not.toContain('playerId=');
}

function waitForSubmitAnswerRequest(page: Page, roomCode: string) {
  return page.waitForRequest((request) =>
    request.method() === 'POST'
    && request.url().includes(`/api/rooms/${roomCode}/ranking-game/rounds/`)
    && request.url().endsWith('/answers')
  );
}

function assertSubmitAnswerRequestUsesSessionTokenOnly(submitAnswerRequest: Request): void {
  expect(submitAnswerRequest.headerValue(PLAYER_SESSION_TOKEN_HEADER)).toBeTruthy();
  expect(submitAnswerRequest.postDataJSON()).toEqual({
    answerText: ANSWER_TEXT,
  });
  expect(submitAnswerRequest.postData() ?? '').not.toContain('playerId');
  expect(submitAnswerRequest.postData() ?? '').not.toContain('hostId');
  expect(submitAnswerRequest.url()).not.toContain('playerId=');
}

function assertProtectedGetRequestUsesSessionTokenOnly(request: Request, roomCode: string, suffix = ''): void {
  expect(request.headerValue(PLAYER_SESSION_TOKEN_HEADER)).toBeTruthy();
  expect(request.url()).toContain(`/api/rooms/${roomCode}${suffix}`);
  expect(request.url()).not.toContain('playerId=');
  expect(request.url()).not.toContain('role=');
}

async function expectStoredTokenToExist(page: Page): Promise<void> {
  const playerData = await readStoredPlayerData(page);
  expect(playerData?.playerSessionToken).toBeTruthy();
}

async function overwriteStoredToken(page: Page, token: string): Promise<void> {
  await page.evaluate(([storageKey, nextToken]) => {
    const raw = sessionStorage.getItem(storageKey);
    if (!raw) {
      throw new Error('No stored playerData found in sessionStorage.');
    }

    const parsed = JSON.parse(raw) as StoredPlayerData;
    sessionStorage.setItem(storageKey, JSON.stringify({
      ...parsed,
      playerSessionToken: nextToken,
    }));
  }, [PLAYER_DATA_STORAGE_KEY, token] as const);
}

async function readStoredPlayerData(page: Page): Promise<StoredPlayerData | null> {
  return page.evaluate((storageKey) => {
    const raw = sessionStorage.getItem(storageKey);
    if (!raw) {
      return null;
    }

    return JSON.parse(raw) as StoredPlayerData;
  }, PLAYER_DATA_STORAGE_KEY);
}

async function expectErrorPage(
  page: Page,
  expectation: ErrorPageExpectation,
): Promise<void> {
  await page.waitForURL(/\/error$/);
  await expect(page.getByRole('heading', {name: expectation.title})).toBeVisible();
  await expect(page.locator('.message')).toContainText(expectation.message);
  await expect(page.locator('.details')).toContainText(expectation.status);
  await expect(page.locator('.details')).toContainText(expectation.detailMessage);
}
