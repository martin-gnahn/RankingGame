import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { GameApiService } from './game-api.service';

describe('GameApiService', () => {
  let service: GameApiService;
  let httpTesting: HttpTestingController;
  let consoleLogSpy: jasmine.Spy;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [GameApiService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(GameApiService);
    httpTesting = TestBed.inject(HttpTestingController);
    consoleLogSpy = spyOn(console, 'log');
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should load active game players with an encoded room code and log the DTO response', () => {
    const response = [
      {
        gameSessionId: 'session-1',
        playerId: 'player-1',
      },
      {
        gameSessionId: 'session-1',
        playerId: 'player-2',
      },
    ];

    service.getActivePlayers('A/B1').subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = httpTesting.expectOne(
      `${environment.apiBaseUrl}/rooms/A%2FB1/ranking-game/current-round/players`,
    );
    expect(request.request.method).toBe('GET');

    request.flush(response);

    expect(consoleLogSpy).toHaveBeenCalledOnceWith('Active game players DTO response', response);
  });
});
