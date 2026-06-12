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

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
