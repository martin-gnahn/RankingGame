# Debugging Playwright Tests

This project uses Playwright for browser E2E tests in `frontend/e2e/`.

On Windows PowerShell, use `npm.cmd` and `npx.cmd`. Plain `npm`/`npx` may be blocked by the local script execution policy.

## 1. One-Time Browser Install

From `frontend/`:

```powershell
npm.cmd run e2e:install
```

This installs the Chromium browser used by the default E2E scripts.

## 2. Start The Backend For Room-Flow Tests

The smoke test only needs the Angular app. Room-flow tests also need PostgreSQL and the Spring Boot backend.

From the repository root:

```powershell
docker compose up -d
.\mvnw.cmd spring-boot:run
```

Leave the backend terminal running while debugging Playwright.

## 3. Recommended Debugging Workflow

For most frontend/E2E debugging, use Playwright UI Mode with a visible browser:

```powershell
npm.cmd run e2e:ui -- --headed
```

For a specific spec:

```powershell
npm.cmd run e2e:ui -- e2e/room-create-join.spec.ts --headed
```

This is the recommended default while learning or debugging the application flow because it lets you:

* run individual tests from the Playwright UI
* see the real browser
* inspect Playwright actions step by step
* check console logs, network calls, screenshots, errors and locators
* rerun tests quickly after code changes

Mental model:

```text
--ui       = interactive Playwright test runner
--headed   = show the real browser window
--debug    = focused Playwright Inspector/debug session
debugger;  = browser DevTools breakpoint in productive frontend code
```

## 4. Open UI Mode For A Specific Spec

From `frontend/`:

```powershell
npm.cmd run e2e:ui -- e2e/room-create-join.spec.ts
```

Useful examples:

```powershell
npm.cmd run e2e:ui -- e2e/smoke.spec.ts
npm.cmd run e2e:ui -- e2e/room-start-lobby.spec.ts
npm.cmd run e2e:ui -- e2e/game-answer-submission.spec.ts
```

With a real browser window:

```powershell
npm.cmd run e2e:ui -- e2e/room-create-join.spec.ts --headed
```

UI Mode will start the Angular dev server automatically on `http://localhost:4200`.

The `e2e:ui` script includes `--timeout=0`, so Playwright's per-test timeout is disabled while you pause at frontend or backend breakpoints.

## 5. Run One Test By Name

Use `-g` to filter by test title:

```powershell
npm.cmd run e2e:ui -- e2e/room-start-lobby.spec.ts -g "shows an error when the host starts"
```

With a visible browser:

```powershell
npm.cmd run e2e:ui -- e2e/room-start-lobby.spec.ts -g "shows an error when the host starts" --headed
```

If the title contains quotes or special characters, copy only a stable substring.

Example:

```powershell
npm.cmd run e2e:ui -- e2e/room-start-lobby.spec.ts -g "host starts"
```

## 6. Pause A Test At A Specific Point

If the test runs too fast, add `page.pause()` to the test:

```ts
test('starts the game from the lobby', async ({ page }) => {
  await page.goto('/');

  await page.pause(); // pause here to inspect the browser or open DevTools

  await page.getByRole('button', { name: 'Start game' }).click();
});
```

Then run:

```powershell
npm.cmd run e2e:ui -- e2e/room-start-lobby.spec.ts --headed
```

This pauses Playwright test execution. From there you can inspect the page, open browser DevTools, and then continue the test.

Use `page.pause()` when:

* the test is too fast
* you want to inspect the current DOM state
* you want time to open DevTools
* you want to continue manually from a known point

Remove `page.pause()` before committing unless it is intentionally needed for local debugging.

## 7. Debug Productive Frontend Code With `debugger;`

To stop inside productive Angular code, use a normal JavaScript `debugger;` statement:

```ts
startGame(): void {
  debugger;

  this.gameService.startGame();
}
```

Important: `debugger;` is handled by **browser DevTools**, not by the Playwright UI itself.

Recommended workflow:

1. Add `page.pause()` shortly before the relevant user action in the Playwright test.

   ```ts
   await page.pause();

   await page.getByRole('button', { name: 'Start game' }).click();
   ```

2. Add `debugger;` inside the productive frontend method you want to inspect.

   ```ts
   startGame(): void {
     debugger;
     this.gameService.startGame();
   }
   ```

3. Start UI Mode with a visible browser:

   ```powershell
   npm.cmd run e2e:ui -- e2e/room-start-lobby.spec.ts --headed
   ```

4. Start the test in Playwright UI Mode.

5. When the test pauses at `page.pause()`, open DevTools in the browser window:

   ```text
   F12
   ```

   or right click -> Inspect.

6. Continue the test.

7. When the productive code reaches `debugger;`, Chrome DevTools stops there.

This is usually the best setup for understanding how an E2E action reaches Angular components, services, HTTP calls or WebSocket logic.

## 8. Optionally Open DevTools Automatically

For Chromium, DevTools can be opened automatically via Playwright config:

```ts
export default defineConfig({
  use: {
    headless: false,
    launchOptions: {
      devtools: true,
    },
  },
});
```

Alternative:

```ts
export default defineConfig({
  use: {
    headless: false,
    launchOptions: {
      args: ['--auto-open-devtools-for-tabs'],
    },
  },
});
```

Use this only temporarily while debugging. It is usually not needed as permanent project config.

## 9. Watch The UI Interactions

In Playwright UI Mode:

1. Select the spec in the left panel.
2. Click the play button beside the test you want.
3. Watch the browser panel or the real browser window.
4. Use the timeline/action list to jump to a specific step.
5. Inspect the action details, locator, console, network, screenshots and errors from the UI Mode panels.

For room tests, you should see the browser create or join a room, move to `/lobby/...`, and sometimes navigate to `/game/...`.

## 10. Debug With Playwright Inspector

For slower, focused step-by-step debugging with Playwright Inspector:

```powershell
npm.cmd run e2e:debug -- e2e/room-create-join.spec.ts
```

To debug one named test:

```powershell
npm.cmd run e2e:debug -- e2e/room-start-lobby.spec.ts -g "every non-host player is disconnected"
```

This opens a browser and pauses execution so you can step through each Playwright action.

The `e2e:debug` script also includes `--timeout=0`.

Use this mode when:

* one specific test fails
* you want to step through Playwright actions slowly
* you already know the test you want to inspect
* UI Mode feels too broad for the current problem

For general learning and architecture understanding, prefer:

```powershell
npm.cmd run e2e:ui -- --headed
```

For surgical debugging of one failing test, prefer:

```powershell
npm.cmd run e2e:debug -- e2e/some-file.spec.ts -g "part of test title"
```

## 11. Reuse An Already Running Angular Server

If you started Angular yourself:

```powershell
npm.cmd run start
```

Run Playwright without starting a second Angular server:

```powershell
$env:PLAYWRIGHT_SKIP_WEB_SERVER="1"
npm.cmd run e2e:ui -- e2e/room-create-join.spec.ts
```

To target a different frontend URL:

```powershell
$env:PLAYWRIGHT_BASE_URL="http://localhost:4300"
npm.cmd run e2e:ui -- e2e/smoke.spec.ts
```

To reset the environment variables in the same PowerShell session:

```powershell
Remove-Item Env:\PLAYWRIGHT_SKIP_WEB_SERVER
Remove-Item Env:\PLAYWRIGHT_BASE_URL
```

## 12. Open Failure Artifacts

After a failed run:

```powershell
npm.cmd run e2e:report
```

Open a retained trace:

```powershell
npm.cmd run e2e:trace -- test-results\<failed-test-folder>\trace.zip
```

The trace viewer is useful when a test fails too quickly to understand from the live browser view. It lets you replay every action, DOM snapshot, network request, screenshot and console message.

## 13. Common Commands

```powershell
# Run the default Chromium E2E suite
npm.cmd run e2e

# Run only the smoke test
npm.cmd run e2e:smoke

# Open UI Mode
npm.cmd run e2e:ui

# Open UI Mode with a real browser
npm.cmd run e2e:ui -- --headed

# Open UI Mode for one file
npm.cmd run e2e:ui -- e2e/room-create-join.spec.ts

# Open UI Mode for one file with a real browser
npm.cmd run e2e:ui -- e2e/room-create-join.spec.ts --headed

# Run one named test in UI Mode
npm.cmd run e2e:ui -- e2e/room-start-lobby.spec.ts -g "host starts"

# Debug one file with Playwright Inspector
npm.cmd run e2e:debug -- e2e/room-create-join.spec.ts

# Debug one named test with Playwright Inspector
npm.cmd run e2e:debug -- e2e/room-start-lobby.spec.ts -g "host starts"

# See Playwright CLI help
npx.cmd playwright help test
```

## 14. Personal Rule Of Thumb

Use this most of the time:

```powershell
npm.cmd run e2e:ui -- --headed
```

Use this when a test is too fast:

```ts
await page.pause();
```

Use this to stop inside productive frontend code:

```ts
debugger;
```

Use this for a focused debugging session of one specific failing test:

```powershell
npm.cmd run e2e:debug -- e2e/some-file.spec.ts -g "part of test title"
```

Use the trace viewer when the failure already happened and you want to inspect it afterwards:

```powershell
npm.cmd run e2e:report
```
