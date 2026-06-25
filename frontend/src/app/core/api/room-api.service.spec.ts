import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { RoomApiService } from './room-api.service';

describe('RoomApiService', () => {
  let service: RoomApiService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [RoomApiService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(RoomApiService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should create a room with the expected URL and payload', () => {
    const response = { roomCode: 'ABCD', playerId: '7' };

    service.createRoom({ playerName: 'Marta' }).subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = httpTesting.expectOne(`${environment.apiBaseUrl}/rooms`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ playerName: 'Marta' });

    request.flush(response);
  });

  it('should join a room with an encoded room code and expected payload', () => {
    const response = { roomCode: 'A/B1', playerId: '9' };

    service.joinRoom('A/B1', { playerName: 'Alex' }).subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = httpTesting.expectOne(`${environment.apiBaseUrl}/rooms/A%2FB1/players`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ playerName: 'Alex' });

    request.flush(response);
  });

  it('should load room details with an encoded room code', () => {
    const response = {
      roomId: 'room-1',
      roomCode: 'A/B1',
      status: 'LOBBY' as const,
      canStartGame: false,
      startBlockedReason: 'At least 2 players are required to start the game',
      players: [
        {
          playerId: 'player-1',
          nickname: 'Marta',
          host: true,
          connectionStatus: 'CONNECTED' as const,
        },
      ],
    };

    service.getRoom('A/B1').subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = httpTesting.expectOne(`${environment.apiBaseUrl}/rooms/A%2FB1`);
    expect(request.request.method).toBe('GET');

    request.flush(response);
  });

  it('should load recent chat messages with an encoded room code', () => {
    const response = [
      {
        messageId: 'message-1',
        playerId: 'player-1',
        senderNickname: 'Marta',
        body: 'Hallo',
        createdAt: '2026-06-24T10:15:30Z',
      },
    ];

    service.getRecentChatMessages('A/B1').subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = httpTesting.expectOne(`${environment.apiBaseUrl}/rooms/A%2FB1/chat/messages`);
    expect(request.request.method).toBe('GET');

    request.flush(response);
  });

  it('should start a ranking game with an encoded room code and host player id', () => {
    const response = {
      roomId: 'room-1',
      roomCode: 'A/B1',
      gameSessionId: 'session-1',
      gameType: 'RANKING_GAME' as const,
      roundId: 'round-1',
      roundNumber: 1,
      questionId: 'question-1',
    };

    service.startRankingGame('A/B1', { hostPlayerId: 'host-1' }).subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = httpTesting.expectOne(
      `${environment.apiBaseUrl}/rooms/A%2FB1/ranking-game/start`,
    );
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ hostPlayerId: 'host-1' });

    request.flush(response);
  });

  it('should load the active round with an encoded room code', () => {
    const response = {
      roomId: 'room-1',
      roomCode: 'A/B1',
      gameSessionId: 'session-1',
      roundId: 'round-1',
      roundNumber: 1,
      questionId: 'question-1',
      questionText: 'Welche Ausrede funktioniert immer?',
      assignedCardValue: 7,
    };

    service.getActiveRound('A/B1', 'player-1').subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = httpTesting.expectOne(
      `${environment.apiBaseUrl}/rooms/A%2FB1/ranking-game/current-round?playerId=player-1`,
    );
    expect(request.request.method).toBe('GET');

    request.flush(response);
  });

  it('should submit an answer with an encoded room code and round id', () => {
    const response = {
      answerId: 'answer-1',
      roundId: 'round/1',
      playerId: 'player-1',
      submitted: true,
    };

    service
      .submitAnswer('A/B1', 'round/1', {
        playerId: 'player-1',
        answerText: 'Mit WLAN-Problemen.',
      })
      .subscribe((result) => {
        expect(result).toEqual(response);
      });

    const request = httpTesting.expectOne(
      `${environment.apiBaseUrl}/rooms/A%2FB1/ranking-game/rounds/round%2F1/answers`,
    );
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      playerId: 'player-1',
      answerText: 'Mit WLAN-Problemen.',
    });

    request.flush(response);
  });
});
