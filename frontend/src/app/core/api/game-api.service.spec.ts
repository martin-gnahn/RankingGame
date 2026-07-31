import {provideHttpClient} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {TestBed} from '@angular/core/testing';

import {environment} from '../../../environments/environment';
import {RankAnswerResultResponse, RankedAnswerListResponse} from './game.models';
import {GameApiService} from './game-api.service';

describe('GameApiService', () => {
  let service: GameApiService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [GameApiService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(GameApiService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should load active game players with an encoded room code', () => {
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
  });

  it('should load submitted answers for the current round', () => {
    const response = {
      answers: [
        {
          answerId: 'answer-1',
          playerId: 'player-1',
          nickname: 'Marta',
          answerText: 'Mit WLAN-Problemen.',
          cardValue: 7 as const,
        },
      ],
    };

    service.getSubmittedAnswers('A/B1', 'round-1').subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = httpTesting.expectOne(
      `${environment.apiBaseUrl}/rooms/A%2FB1/ranking-game/rounds/round-1/answers`,
    );
    expect(request.request.method).toBe('GET');

    request.flush(response);
  });

  it('should add a ranking position with the answer id', () => {
    const response: RankAnswerResultResponse = {
      roundId: 'round-1',
      playerId: 'player-2',
      answerId: 'answer-1',
      rankingId: 'ranking-1',
      oneBasedPosition: 1,
    };

    service
      .addRankingPosition('A/B1', 'round-1', {
        answerId: 'answer-1',
      })
      .subscribe((result) => {
        expect(result).toEqual(response);
      });

    const request = httpTesting.expectOne(
      `${environment.apiBaseUrl}/rooms/A%2FB1/ranking-game/rounds/round-1/answer/position/new`,
    );
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      answerId: 'answer-1',
    });

    request.flush(response);
  });

  it('should return the current ranking positions wrapper', () => {
    const response: RankedAnswerListResponse = {
      rankings: [
        {
          rankingId: 'ranking-1',
          answerId: 'answer-1',
          playerId: 'player-2',
          answerText: 'Im Aufzug stecken geblieben.',
          oneBasedPosition: 1,
        },
      ],
    };

    service.getRankingPositions('A/B1', 'round-1').subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = httpTesting.expectOne(
      `${environment.apiBaseUrl}/rooms/A%2FB1/ranking-game/rounds/round-1/answer/position/all`,
    );
    expect(request.request.method).toBe('GET');

    request.flush(response);
  });
});
