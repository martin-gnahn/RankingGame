import {HttpInterceptorFn} from '@angular/common/http';
import {inject} from '@angular/core';

import {environment} from '../../../environments/environment';
import {PlayerSessionStore} from '../../shared/player-session-store';

const PLAYER_SESSION_TOKEN_HEADER = 'X-Player-Session-Token';

export const playerSessionTokenInterceptor: HttpInterceptorFn = (request, next) => {
  const token = inject(PlayerSessionStore).playerSessionToken();

  if (!shouldAttachToken(request.url, token)) {
    return next(request);
  }

  return next(request.clone({
    setHeaders: {
      [PLAYER_SESSION_TOKEN_HEADER]: token,
    },
  }));
};

function isValidToken(token: string | null) {
  return typeof token === 'string'
    && token.trim().length > 0;
}

function shouldAttachToken(url: string, token: string | null): token is string {
  return url.startsWith(environment.apiBaseUrl) && isValidToken(token);
}
