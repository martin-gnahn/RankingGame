import {provideHttpClient, withInterceptors} from '@angular/common/http';
import {ApplicationConfig, provideBrowserGlobalErrorListeners, provideZoneChangeDetection} from '@angular/core';
import {provideAnimationsAsync} from '@angular/platform-browser/animations/async';
import {provideRouter} from '@angular/router';

import {routes} from './app.routes';
import {playerSessionTokenInterceptor} from './core/api/player-session-token.interceptor';
import {provideAppTranslations} from './core/i18n/app-translate.providers';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideAnimationsAsync(),
    provideHttpClient(withInterceptors([playerSessionTokenInterceptor])),
    provideAppTranslations(),
    provideRouter(routes)
  ]
};
