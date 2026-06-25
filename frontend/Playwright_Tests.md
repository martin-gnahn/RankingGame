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

## 3. Open UI Mode For A Specific Spec

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

UI mode will start the Angular dev server automatically on `http://localhost:4200`.
The `e2e:ui` script includes `--timeout=0`, so Playwright's per-test timeout is disabled while you pause at backend breakpoints.

## 4. Run One Test By Name

Use `-g` to filter by test title:

```powershell
npm.cmd run e2e:ui -- e2e/room-start-lobby.spec.ts -g "shows an error when the host starts"
```

If the title contains quotes or special characters, copy only a stable substring.

## 5. Watch The UI Interactions

In Playwright UI mode:

1. Select the spec in the left panel.
2. Click the play button beside the test you want.
3. Watch the browser panel as Playwright fills fields, clicks buttons, and navigates.
4. Use the timeline/action list to jump to a specific step.
5. Inspect the action details, locator, console, network, screenshots, and errors from the UI mode panels.

For room tests, you should see the browser create or join a room, move to `/lobby/...`, and sometimes navigate to `/game/...`.

## 6. Debug In A Real Browser Window

For slower step-by-step debugging with Playwright Inspector:

```powershell
npm.cmd run e2e:debug -- e2e/room-create-join.spec.ts
```

To debug one named test:

```powershell
npm.cmd run e2e:debug -- e2e/room-start-lobby.spec.ts -g "every non-host player is disconnected"
```

This opens a browser and pauses execution so you can step through each Playwright action. The `e2e:debug` script also includes `--timeout=0`.

## 7. Reuse An Already Running Angular Server

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

## 8. Open Failure Artifacts

After a failed run:

```powershell
npm.cmd run e2e:report
```

Open a retained trace:

```powershell
npm.cmd run e2e:trace -- test-results\<failed-test-folder>\trace.zip
```

The trace viewer is useful when a test fails too quickly to understand from the live browser view. It lets you replay every action, DOM snapshot, network request, screenshot, and console message.

## 9. Common Commands

```powershell
# Run the default Chromium E2E suite
npm.cmd run e2e

# Run only the smoke test
npm.cmd run e2e:smoke

# Open UI mode for one file
npm.cmd run e2e:ui -- e2e/room-create-join.spec.ts

# Debug one file with Playwright Inspector
npm.cmd run e2e:debug -- e2e/room-create-join.spec.ts

# See Playwright CLI help
npx.cmd playwright help test
```
