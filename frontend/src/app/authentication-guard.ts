import {CanActivateFn, Router} from '@angular/router';
import {inject} from '@angular/core';
import {PlayerSessionStore} from './shared/player-session-store';
import {ErrorDataWriterService} from './error-data-writer-service';
import {HttpStatusCode} from '@angular/common/http';
import {NO_TOKEN_KEY} from './error/error';

const NO_USER_TOKEN_MESSAGE = 'No user authentication token.';

export const authenticationGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const playerSessionStore = inject(PlayerSessionStore);
  if (!playerSessionStore.playerSessionToken()) {
    inject(ErrorDataWriterService).errorData.set({
      status: HttpStatusCode.Unauthorized,
      errorKey: NO_TOKEN_KEY,
      message: NO_USER_TOKEN_MESSAGE,
    });
    return router.createUrlTree(['/error']);
  }

  return true;
};
