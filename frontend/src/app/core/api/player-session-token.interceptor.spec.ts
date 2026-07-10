import {HttpClient, provideHttpClient, withInterceptors} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {TestBed} from '@angular/core/testing';

import {environment} from '../../../environments/environment';
import {PlayerSessionStore} from '../../shared/player-session-store';
import {playerSessionTokenInterceptor} from './player-session-token.interceptor';

describe('playerSessionTokenInterceptor', () => {
  let http: HttpClient;
  let httpTesting: HttpTestingController;
  let playerSessionStore: PlayerSessionStore;

  beforeEach(() => {
    sessionStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([playerSessionTokenInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
    playerSessionStore = TestBed.inject(PlayerSessionStore);
  });

  afterEach(() => {
    httpTesting.verify();
    sessionStorage.clear();
  });

  it('adds the player session token to API requests when a token exists', () => {
    playerSessionStore.storePlayerData({
      playerId: 'player-1',
      role: 'host',
      playerSessionToken: 'secret-token',
    });

    http.get(`${environment.apiBaseUrl}/rooms/ABCD12`).subscribe();

    const request = httpTesting.expectOne(`${environment.apiBaseUrl}/rooms/ABCD12`);
    expect(request.request.headers.get('X-Player-Session-Token')).toBe('secret-token');

    request.flush({});
  });

  it('does not add an empty token header when no token exists', () => {
    http.get(`${environment.apiBaseUrl}/rooms/ABCD12`).subscribe();

    const request = httpTesting.expectOne(`${environment.apiBaseUrl}/rooms/ABCD12`);
    expect(request.request.headers.has('X-Player-Session-Token')).toBeFalse();

    request.flush({});
  });

  it('does not add the token to non-api requests', () => {
    playerSessionStore.storePlayerData({
      playerId: 'player-1',
      role: 'host',
      playerSessionToken: 'secret-token',
    });

    http.get('/assets/config.json').subscribe();

    const request = httpTesting.expectOne('/assets/config.json');
    expect(request.request.headers.has('X-Player-Session-Token')).toBeFalse();

    request.flush({});
  });
});
