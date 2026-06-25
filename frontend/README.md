# Frontend

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 20.3.28.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
npm run build
```

This will compile your project and store the production browser artifacts in
`dist/frontend/browser`. By default, the production build optimizes your application for
performance and speed.

## Vercel deployment

This frontend can be deployed independently from the Spring Boot backend. In Vercel, create a
project for this repository with these settings:

| Setting | Value |
| --- | --- |
| Root Directory | `frontend` |
| Install Command | `npm ci` |
| Build Command | `npm run build` |
| Output Directory | `dist/frontend/browser` |

The local `vercel.json` mirrors the install, build, output, and single-page app rewrite settings.
The rewrite sends unknown paths to `index.html`, so future Angular client routes can be refreshed
or opened directly.

If Vercel does not offer `frontend` as a selectable root directory, leave Root Directory as `./`.
The repository-level `vercel.json` delegates the install and build commands into `frontend`:

| Setting | Value |
| --- | --- |
| Root Directory | `./` |
| Install Command | `npm ci --prefix frontend` |
| Build Command | `npm run build --prefix frontend` |
| Output Directory | `frontend/dist/frontend/browser` |

Backend and database deployment are intentionally out of scope for this frontend-only deployment.
`docker-compose.yml` remains local development infrastructure and is not required by Vercel.

Backend API URL handling is deferred until the backend deployment ticket. The current frontend does
not require a backend URL to build or deploy; when API calls are introduced, configure the base URL
through Angular/Vercel environment handling instead of hard-coding a local Spring Boot address.

## Running unit tests

To execute unit tests with the [Karma](https://karma-runner.github.io) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

End-to-end tests use [Playwright](https://playwright.dev/) and live in `e2e/`.

Install the Chromium browser once after installing dependencies:

```bash
npm run e2e:install
```

Run the E2E suite headlessly in Chromium:

```bash
npm run e2e
```

Run only the frontend smoke check:

```bash
npm run e2e:smoke
```

Open Playwright UI mode for local debugging:

```bash
npm run e2e:ui
```

Run a single spec in headed debug mode:

```bash
npm run e2e:debug -- e2e/room-create-join.spec.ts
```

`npm run e2e` starts the Angular dev server automatically on `http://localhost:4200`.
Set `PLAYWRIGHT_SKIP_WEB_SERVER=1` if you already have Angular running, or set
`PLAYWRIGHT_BASE_URL` to target a different frontend URL.

Room-flow E2E tests also need the local backend stack:

1. Start PostgreSQL from the repository root with `docker compose up -d`.
2. Start the Spring Boot backend from the repository root with `.\mvnw.cmd spring-boot:run`
   on Windows PowerShell, or `./mvnw spring-boot:run` in Git Bash/macOS/Linux.
3. Run Playwright from this `frontend` directory with `npm run e2e`.

Playwright writes HTML reports to `playwright-report/` and failure artifacts to `test-results/`.
Use `npm run e2e:report` to reopen the HTML report. Use `npm run e2e:trace -- <trace.zip>`
to inspect a retained trace from a failed run.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
