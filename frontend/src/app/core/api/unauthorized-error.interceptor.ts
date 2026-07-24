import {HttpErrorResponse, HttpInterceptorFn, HttpStatusCode} from "@angular/common/http";
import {inject} from "@angular/core";
import {ErrorDataWriterService} from "../../error-data-writer-service";
import {Router} from "@angular/router";
import {catchError, throwError} from "rxjs";
import {PlayerSessionStore} from "../../shared/player-session-store";

const TOKEN_NOT_AUTHORIZED = 'TOKEN_NOT_AUTHORIZED';

interface ApiErrorResponse {
  errorKey?: string;
  message?: string;
}

export const unauthorizedErrorInterceptor: HttpInterceptorFn = (request, next) => {
  const writingErrorDataService = inject(ErrorDataWriterService);
  const router = inject(Router);
  const playerSessionStore = inject(PlayerSessionStore);
  return next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse) {
        const isUnauthorized = error.status === HttpStatusCode.Unauthorized;
        if (isUnauthorized) {
          const errorInfo = error.error as ApiErrorResponse;
          writingErrorDataService.errorData.set({
            status: error.status,
            errorKey: errorInfo.errorKey ?? TOKEN_NOT_AUTHORIZED,
            message: errorInfo.message ?? '',
          });

          void router.navigate(['/error']);
          playerSessionStore.clearPlayerData();
        }
      }

      return throwError(() => error);
    })
  );
};
