import {HttpErrorResponse, provideHttpClient} from '@angular/common/http';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ActivatedRoute, convertToParamMap, provideRouter, Router} from '@angular/router';
import {BehaviorSubject, Observable, of, Subject, throwError} from 'rxjs';

import {RoomApiService} from '../core/api/room-api.service';
import {RoomResponse} from '../core/api/room.models';
import {provideTestingTranslations} from '../core/i18n/translate-testing.providers';
import {RealtimeEvent} from '../core/websocket/web-socket.models';
import {WebSocketService} from '../core/websocket/web-socket.service';
import {PlayerSessionStore} from '../shared/player-session-store';
import {Lobby} from './lobby';

describe('Lobby', () => {
  let fixture: ComponentFixture<Lobby>;
  let roomApi: jasmine.SpyObj<RoomApiService>;
  let webSocket: jasmine.SpyObj<WebSocketService>;
  let router: Router;
  let playerSessionStore: PlayerSessionStore;
  let paramMap: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let queryParamMap: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let realtimeEvents: Subject<RealtimeEvent>;

  const roomResponse: RoomResponse = {
    roomId: 'room-1',
    roomCode: 'ABCD12',
    status: 'LOBBY',
    canStartGame: true,
    startBlockedReason: null,
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
        connectionStatus: 'CONNECTED',
      },
    ],
  };

  beforeEach(async () => {
    sessionStorage.clear();

    roomApi = jasmine.createSpyObj<RoomApiService>('RoomApiService', [
      'getRecentChatMessages',
      'getRoom',
      'startRankingGame',
    ]);
    webSocket = jasmine.createSpyObj<WebSocketService>('WebSocketService', [
      'disconnect',
      'joinLive',
      'sendChatMessage',
      'subscribeToRoom',
    ]);
    realtimeEvents = new Subject<RealtimeEvent>();
    webSocket.subscribeToRoom.and.returnValue(realtimeEvents.asObservable());
    roomApi.getRecentChatMessages.and.returnValue(of([]));
    paramMap = new BehaviorSubject(convertToParamMap({ roomCode: 'ABCD12' }));
    queryParamMap = new BehaviorSubject(convertToParamMap({ playerId: 'host-1', role: 'host' }));

    await TestBed.configureTestingModule({
      imports: [Lobby],
      providers: [
        provideHttpClient(),
        provideTestingTranslations(),
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
    playerSessionStore = TestBed.inject(PlayerSessionStore);
    storePlayerSession('host-1', 'host');
    spyOn(router, 'navigate').and.resolveTo(true);
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  function createComponent(): void {
    fixture = TestBed.createComponent(Lobby);
    fixture.detectChanges();
  }

  function storePlayerSession(playerId: string, role: 'host' | 'player'): void {
    playerSessionStore.storePlayerData({
      playerId,
      role,
      playerSessionToken: `${playerId}-token`,
    });
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
    expect(roomApi.getRecentChatMessages).toHaveBeenCalledOnceWith('ABCD12');
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
    expect(compiled.querySelector('.player-row.current-player')?.textContent).toContain('Marta');
  });

  it('should show the start button only for the current host', () => {
    roomApi.getRoom.and.returnValue(of(roomResponse));

    createComponent();

    expect(textContent()).toContain('Spiel starten');

    storePlayerSession('player-2', 'player');
    fixture.detectChanges();

    expect(textContent()).not.toContain('Spiel starten');
    expect(textContent()).toContain('Der Host startet das Spiel.');
  });

  it('should start the game as the current host and navigate to the game page', () => {
    roomApi.getRoom.and.returnValue(of(roomResponse));
    roomApi.startRankingGame.and.returnValue(
      of({
        roomCode: 'ABCD12',
      }),
    );

    createComponent();
    (fixture.nativeElement as HTMLElement)
      .querySelector<HTMLButtonElement>('.primary-button')
      ?.click();
    fixture.detectChanges();

    expect(roomApi.startRankingGame).toHaveBeenCalledOnceWith('ABCD12');
    expect(router.navigate).toHaveBeenCalledOnceWith(['/game', 'ABCD12']);
  });

  it('should disable start when no other player is online', () => {
    const disconnectedGuestRoom: RoomResponse = {
      ...roomResponse,
      canStartGame: false,
      startBlockedReason: 'At least 2 players are required to start the game',
      players: roomResponse.players.map((player) =>
        player.playerId === 'player-2'
          ? { ...player, connectionStatus: 'DISCONNECTED' }
          : player,
      ),
    };

    roomApi.getRoom.and.returnValue(
      of(disconnectedGuestRoom),
    );

    createComponent();

    const startButton = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(
      '.primary-button',
    );
    expect(startButton?.disabled).toBeTrue();
    expect(textContent()).toContain('At least 2 players are required to start the game');
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
    expect(router.navigate).toHaveBeenCalledOnceWith(['/game', 'ABCD12']);
  });

  it('should render and send chat messages', () => {
    roomApi.getRoom.and.returnValue(of(roomResponse));
    roomApi.getRecentChatMessages.and.returnValue(of([
      {
        messageId: 'message-1',
        playerId: 'player-2',
        senderNickname: 'Alex',
        body: 'Hallo Lobby',
        createdAt: '2026-06-24T10:15:30Z',
      },
    ]));

    createComponent();
    expect(textContent()).toContain('Hallo Lobby');

    const textarea = (fixture.nativeElement as HTMLElement).querySelector<HTMLTextAreaElement>(
      '#chat-body',
    );
    textarea!.value = 'Bereit?';
    textarea!.dispatchEvent(new Event('input'));
    (fixture.nativeElement as HTMLElement)
      .querySelector<HTMLFormElement>('.chat-form')!
      .dispatchEvent(new Event('submit'));

    expect(webSocket.sendChatMessage).toHaveBeenCalledOnceWith('ABCD12', 'host-1', 'Bereit?');

    realtimeEvents.next({
      type: 'CHAT_MESSAGE_SENT',
      payload: {
        messageId: 'message-2',
        playerId: 'host-1',
        senderNickname: 'Marta',
        body: 'Los gehts',
        createdAt: '2026-06-24T10:16:30Z',
      },
    });
    fixture.detectChanges();

    expect(textContent()).toContain('Los gehts');
  });

  it('should unsubscribe from room updates without disconnecting the shared websocket', () => {
    const unsubscribeSpy = jasmine.createSpy('unsubscribe');
    roomApi.getRoom.and.returnValue(of(roomResponse));
    webSocket.subscribeToRoom.and.returnValue(
      new Observable<RealtimeEvent>(() => () => unsubscribeSpy()),
    );

    createComponent();
    fixture.destroy();

    expect(unsubscribeSpy).toHaveBeenCalled();
    expect(webSocket.disconnect).not.toHaveBeenCalled();
  });
});
