import {Component, computed, inject} from '@angular/core';
import {RouterLink} from '@angular/router';
import {TranslatePipe} from '@ngx-translate/core';
import {ErrorDataReaderService} from '../error-data-reader-service';

type ErrorKind = 'tokenExpired' | 'invalidToken' | 'noToken' | 'notFound' | 'serverUnavailable' | 'generic';

export const NO_TOKEN_KEY: string = 'NO_TOKEN';
const DEFAULT_STATUS = '500';
const SERVER_UNAVAILABLE_STATUS = '0';
const NOT_FOUND_STATUS = '404';

const ERROR_COPY_KEYS: Record<ErrorKind, { title: string; message: string }> = {
  noToken: {
    title: 'error.noToken.title',
    message: 'error.noToken.message',
  },
  tokenExpired: {
    title: 'error.tokenExpired.title',
    message: 'error.tokenExpired.message',
  },
  invalidToken: {
    title: 'error.invalidToken.title',
    message: 'error.invalidToken.message',
  },
  notFound: {
    title: 'error.notFound.title',
    message: 'error.notFound.message',
  },
  serverUnavailable: {
    title: 'error.serverUnavailable.title',
    message: 'error.serverUnavailable.message',
  },
  generic: {
    title: 'error.generic.title',
    message: 'error.generic.message',
  },
};

const ERROR_KIND_BY_ERROR_KEY: Record<string, ErrorKind> = {
  [NO_TOKEN_KEY]: 'noToken',
  TOKEN_EXPIRED: 'tokenExpired',
  TOKEN_NOT_AUTHORIZED: 'invalidToken',
  ROOM_NOT_FOUND: 'notFound',
  RESOURCE_NOT_FOUND: 'notFound',
  ROOM_CODE_UNAVAILABLE: 'serverUnavailable',
  QUESTION_UNAVAILABLE: 'serverUnavailable',
};

@Component({
  selector: 'app-error',
  imports: [RouterLink, TranslatePipe],
  templateUrl: './error.html',
  styleUrl: './error.scss',
})
export class ErrorComponent {
  protected readonly homeLink = '/';
  private readonly errorDataReader = inject(ErrorDataReaderService);

  protected readonly displayStatus = computed(() =>
    this.toDisplayValue(this.errorDataReader.errorStatus()) ?? DEFAULT_STATUS
  );
  protected readonly displayMessage = computed(() =>
    this.toDisplayValue(this.errorDataReader.errorMessage()) ?? ''
  );
  private readonly errorKind = computed<ErrorKind>(() => {
    const errorKey = this.errorDataReader.errorKey()?.toUpperCase();
    debugger;
    if (errorKey && ERROR_KIND_BY_ERROR_KEY[errorKey]) {
      return ERROR_KIND_BY_ERROR_KEY[errorKey];
    }

    const status = this.displayStatus();
    if (status === SERVER_UNAVAILABLE_STATUS) {
      return 'serverUnavailable';
    }

    if (status === NOT_FOUND_STATUS) {
      return 'notFound';
    }

    return 'generic';
  });
  protected readonly titleKey = computed(() => ERROR_COPY_KEYS[this.errorKind()].title);
  protected readonly messageKey = computed(() => ERROR_COPY_KEYS[this.errorKind()].message);

  private toDisplayValue(value: string | number | null | undefined): string | null {
    if (value === null || value === undefined) {
      return null;
    }

    const normalized = String(value).trim();
    return normalized.length > 0 ? normalized : null;
  }
}
