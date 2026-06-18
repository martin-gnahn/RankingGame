import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { Router, provideRouter } from '@angular/router';
import { BehaviorSubject, Subject, of, throwError } from 'rxjs';

import { RoomApiService } from '../core/api/room-api.service';
import { RoomResponse } from '../core/api/room.models';
import { RealtimeEvent } from '../core/websocket/web-socket.models';
import { WebSocketService } from '../core/websocket/web-socket.service';
import { Lobby } from './lobby';

describe('Lobby', () => {
  let fixture: ComponentFixture<Lobby>;
  let roomApi: jasmine.SpyObj<RoomApiService>;
  let webSocket: jasmine.SpyObj<WebSocketService>;
  let router: Router;
  let paramMap: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let queryParamMap: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let realtimeEvents: Subject<RealtimeEvent>;

  const roomResponse: RoomResponse = {
    roomId: 'room-1',
    roomCode: 'ABCD12',
    status: 'LOBBY',
    players: [
      {
        playerId: 'host-1',
        nickname: 'Marta',
        host: true,
        connectionStatus: 'CONNECTED',
      },
      {
        playerId: 'player-2',
        nickname: 'Alex',
        host: false,
        connectionStatus: 'DISCONNECTED',
      },
    ],
  };

  beforeEach(async () => {
    roomApi = jasmine.createSpyObj<RoomApiService>('RoomApiService', [
      'getRoom',
      'startRankingGame',
    ]);
    webSocket = jasmine.createSpyObj<WebSocketService>('WebSocketService', [
      'disconnect',
      'joinLive',
      'subscribeToRoom',
    ]);
    realtimeEvents = new Subject<RealtimeEvent>();
    webSocket.subscribeToRoom.and.returnValue(realtimeEvents.asObservable());
    paramMap = new BehaviorSubject(convertToParamMap({ roomCode: 'ABCD12' }));
    queryParamMap = new BehaviorSubject(convertToParamMap({ playerId: 'host-1', role: 'host' }));

    await TestBed.configureTestingModule({
      imports: [Lobby],
      providers: [
        provideRouter([]),
        { provide: RoomApiService, useValue: roomApi },
        { provide: WebSocketService, useValue: webSocket },
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: paramMap.asObservable(),
            queryParamMap: queryParamMap.asObservable(),
          },
        },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
  });

  function createComponent(): void {
    fixture = TestBed.createComponent(Lobby);
    fixture.detectChanges();
  }

  function textContent(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  it('should create', () => {
    roomApi.getRoom.and.returnValue(of(roomResponse));

    createComponent();

    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should load the room from the route room code', () => {
    roomApi.getRoom.and.returnValue(of(roomResponse));

    createComponent();

    expect(roomApi.getRoom).toHaveBeenCalledOnceWith('ABCD12');
    expect(webSocket.subscribeToRoom).toHaveBeenCalledOnceWith('ABCD12');
    expect(webSocket.joinLive).toHaveBeenCalledOnceWith('ABCD12', 'host-1');
    expect(textContent()).toContain('ABCD12');
  });

  it('should render players with host and connection status', () => {
    roomApi.getRoom.and.returnValue(of(roomResponse));

    createComponent();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(textContent()).toContain('2 Spieler');
    expect(textContent()).toContain('Marta');
    expect(textContent()).toContain('Alex');
    expect(textContent()).toContain('Host');
    expect(textContent()).toContain('Online');
    expect(textContent()).toContain('Getrennt');
    expect(compiled.querySelector('.player-row.current-player')?.textContent).toContain('Marta');
  });

  it('should show the start button only for the current host', () => {
    roomApi.getRoom.and.returnValue(of(roomResponse));

    createComponent();

    expect(textContent()).toContain('Spiel starten');

    queryParamMap.next(convertToParamMap({ playerId: 'player-2', role: 'player' }));
    fixture.detectChanges();

    expect(textContent()).not.toContain('Spiel starten');
    expect(textContent()).toContain('Der Host startet das Spiel.');
  });

  it('should start the game as the current host and navigate to the game page', () => {
    roomApi.getRoom.and.returnValue(of(roomResponse));
    roomApi.startRankingGame.and.returnValue(
      of({
        roomId: 'room-1',
        roomCode: 'ABCD12',
        gameSessionId: 'session-1',
        gameType: 'RANKING_GAME',
        roundId: 'round-1',
        roundNumber: 1,
        questionId: 'question-1',
      }),
    );

    createComponent();
    (fixture.nativeElement as HTMLElement)
      .querySelector<HTMLButtonElement>('.primary-button')
      ?.click();
    fixture.detectChanges();

    expect(roomApi.startRankingGame).toHaveBeenCalledOnceWith('ABCD12', { hostPlayerId: 'host-1' });
    expect(router.navigate).toHaveBeenCalledOnceWith(['/game', 'ABCD12'], {
      queryParams: { playerId: 'host-1', role: 'host' },
    });
  });

  it('should show a start error when starting the game fails', () => {
    roomApi.getRoom.and.returnValue(of(roomResponse));
    roomApi.startRankingGame.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 400,
            error: { message: 'Only the host can start the game' },
          }),
      ),
    );

    createComponent();
    (fixture.nativeElement as HTMLElement)
      .querySelector<HTMLButtonElement>('.primary-button')
      ?.click();
    fixture.detectChanges();

    expect(textContent()).toContain('Only the host can start the game');
  });

  it('should show a loading state while the room request is pending', () => {
    const roomSubject = new Subject<RoomResponse>();
    roomApi.getRoom.and.returnValue(roomSubject.asObservable());

    createComponent();

    expect(textContent()).toContain('Lobby wird geladen');

    roomSubject.next(roomResponse);
    roomSubject.complete();
    fixture.detectChanges();

    expect(textContent()).toContain('Marta');
  });

  it('should show an error state when loading fails', () => {
    roomApi.getRoom.and.returnValue(
      throwError(
        () => new HttpErrorResponse({ status: 404, error: { message: 'Room not found' } }),
      ),
    );

    createComponent();

    expect(textContent()).toContain('Room not found');
    expect(textContent()).toContain('Erneut versuchen');
  });

  it('should refresh the room when a player joined event arrives', () => {
    const updatedRoom: RoomResponse = {
      ...roomResponse,
      players: [
        ...roomResponse.players,
        {
          playerId: 'player-3',
          nickname: 'Sam',
          host: false,
          connectionStatus: 'CONNECTED',
        },
      ],
    };
    roomApi.getRoom.and.returnValues(of(roomResponse), of(updatedRoom));

    createComponent();
    realtimeEvents.next({ type: 'PLAYER_JOINED', payload: { playerId: 'player-3' } });
    fixture.detectChanges();

    expect(roomApi.getRoom).toHaveBeenCalledTimes(2);
    expect(textContent()).toContain('3 Spieler');
    expect(textContent()).toContain('Sam');
  });

  it('should refresh the room when a player left event arrives', () => {
    const updatedRoom: RoomResponse = {
      ...roomResponse,
      players: roomResponse.players.map((player) =>
        player.playerId === 'player-2' ? { ...player, connectionStatus: 'DISCONNECTED' } : player,
      ),
    };
    roomApi.getRoom.and.returnValues(of(roomResponse), of(updatedRoom));

    createComponent();
    realtimeEvents.next({ type: 'PLAYER_LEFT', payload: { playerId: 'player-2' } });
    fixture.detectChanges();

    expect(roomApi.getRoom).toHaveBeenCalledTimes(2);
    expect(textContent()).toContain('Getrennt');
  });

  it('should navigate to the game page when a game started event arrives', () => {
    roomApi.getRoom.and.returnValue(of(roomResponse));

    createComponent();
    realtimeEvents.next({ type: 'GAME_STARTED', payload: { gameSessionId: 'session-1' } });
    fixture.detectChanges();

    expect(roomApi.getRoom).toHaveBeenCalledTimes(1);
    expect(router.navigate).toHaveBeenCalledOnceWith(['/game', 'ABCD12'], {
      queryParams: { playerId: 'host-1', role: 'host' },
    });
  });

  it('should disconnect the websocket when the lobby is destroyed', () => {
    roomApi.getRoom.and.returnValue(of(roomResponse));

    createComponent();
    fixture.destroy();

    expect(webSocket.disconnect).toHaveBeenCalled();
  });
});
