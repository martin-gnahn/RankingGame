import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ActivatedRoute, convertToParamMap, provideRouter} from '@angular/router';
import {BehaviorSubject, Observable, of, Subject, throwError} from 'rxjs';
import {HttpErrorResponse} from '@angular/common/http';

import {GameApiService} from '../core/api/game-api.service';
import {RoomApiService} from '../core/api/room-api.service';
import {ActiveRoundResponse} from '../core/api/room.models';
import {RealtimeEvent} from '../core/websocket/web-socket.models';
import {WebSocketService} from '../core/websocket/web-socket.service';
import {Game} from './game';

describe('Game', () => {
  let fixture: ComponentFixture<Game>;
  let roomApi: jasmine.SpyObj<RoomApiService>;
  let gameApi: jasmine.SpyObj<GameApiService>;
  let webSocket: jasmine.SpyObj<WebSocketService>;
  let paramMap: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let queryParamMap: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let realtimeEvents: Subject<RealtimeEvent>;

  const activeRound: ActiveRoundResponse = {
    roomId: 'room-1',
    roomCode: 'ABCD12',
    gameSessionId: 'session-1',
    roundId: 'round-1',
    roundNumber: 1,
    questionId: 'question-1',
    questionText: 'Welche Ausrede funktioniert immer?',
    assignedCardValue: 7,
  };

  beforeEach(async () => {
    roomApi = jasmine.createSpyObj<RoomApiService>('RoomApiService', [
      'getActiveRound',
      'getRecentChatMessages',
      'submitAnswer',
    ]);
    gameApi = jasmine.createSpyObj<GameApiService>('GameApiService', ['getActivePlayers']);
    webSocket = jasmine.createSpyObj<WebSocketService>('WebSocketService', [
      'disconnect',
      'joinLive',
      'sendChatMessage',
      'subscribeToRoom',
    ]);
    realtimeEvents = new Subject<RealtimeEvent>();
    webSocket.subscribeToRoom.and.returnValue(realtimeEvents.asObservable());
    roomApi.getRecentChatMessages.and.returnValue(of([]));
    gameApi.getActivePlayers.and.returnValue(of([]));
    paramMap = new BehaviorSubject(convertToParamMap({ roomCode: 'ABCD12' }));
    queryParamMap = new BehaviorSubject(convertToParamMap({ playerId: 'player-1' }));

    await TestBed.configureTestingModule({
      imports: [Game],
      providers: [
        provideRouter([]),
        { provide: RoomApiService, useValue: roomApi },
        { provide: GameApiService, useValue: gameApi },
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
  });

  function createComponent(): void {
    roomApi.getActiveRound.and.returnValue(of(activeRound));
    roomApi.submitAnswer.and.returnValue(
      of({ answerId: 'answer-1', roundId: 'round-1', playerId: 'player-1', submitted: true }),
    );
    fixture = TestBed.createComponent(Game);
    fixture.detectChanges();
  }

  function textContent(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  it('should render the active round question', () => {
    createComponent();

    expect(roomApi.getActiveRound).toHaveBeenCalledOnceWith('ABCD12', 'player-1');
    expect(roomApi.getRecentChatMessages).toHaveBeenCalledOnceWith('ABCD12');
    expect(webSocket.subscribeToRoom).toHaveBeenCalledOnceWith('ABCD12');
    expect(webSocket.joinLive).toHaveBeenCalledOnceWith('ABCD12', 'player-1');
    expect(textContent()).toContain('Runde 1');
    expect(textContent()).toContain('Welche Ausrede funktioniert immer?');
    expect(textContent()).toContain('Antwort');
    expect(textContent()).toContain('Deine Karte:');
    expect(compiledAssignedCardText(fixture)).toContain('7');
  });

  it('should highlight the assigned card without a selectable button', () => {
    createComponent();

    const compiled = fixture.nativeElement as HTMLElement;
    const seventhCard = compiled.querySelectorAll<HTMLElement>('.score-card')[6];

    expect(seventhCard.classList).toContain('active');
    expect(seventhCard.getAttribute('aria-label')).toBe('Deine Karte 7');
    expect(seventhCard.textContent).toContain('Deine Karte');
    expect(compiled.querySelector('button.score-card')).toBeNull();
  });

  it('should submit an answer once and show the waiting state', () => {
    createComponent();

    const compiled = fixture.nativeElement as HTMLElement;
    const textarea = compiled.querySelector<HTMLTextAreaElement>('textarea[formControlName="answerText"]');
    textarea!.value = 'Mit WLAN-Problemen.';
    textarea!.dispatchEvent(new Event('input'));
    compiled.querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(roomApi.submitAnswer).toHaveBeenCalledOnceWith('ABCD12', 'round-1', {
      playerId: 'player-1',
      answerText: 'Mit WLAN-Problemen.',
    });
    expect(textContent()).toContain('Antwort gespeichert');

    compiled.querySelector('form')!.dispatchEvent(new Event('submit'));
    expect(roomApi.submitAnswer).toHaveBeenCalledTimes(1);
  });

  it('should require answer text before submitting', () => {
    createComponent();

    const compiled = fixture.nativeElement as HTMLElement;
    compiled.querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(roomApi.submitAnswer).not.toHaveBeenCalled();
    expect(textContent()).toContain('Bitte gib eine Reaktion ein.');
  });

  it('should show an error when the active round cannot be loaded', () => {
    roomApi.getActiveRound.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 400, error: { message: 'No active game is running' } })),
    );
    roomApi.getRecentChatMessages.and.returnValue(of([]));

    fixture = TestBed.createComponent(Game);
    fixture.detectChanges();

    expect(textContent()).toContain('No active game is running');
  });

  it('should render and send chat messages', () => {
    roomApi.getRecentChatMessages.and.returnValue(of([
      {
        messageId: 'message-1',
        playerId: 'player-2',
        senderNickname: 'Alex',
        body: 'Ich bin drin',
        createdAt: '2026-06-24T10:15:30Z',
      },
    ]));
    createComponent();

    expect(textContent()).toContain('Ich bin drin');

    const textarea = (fixture.nativeElement as HTMLElement).querySelector<HTMLTextAreaElement>(
      '#chat-body',
    );
    textarea!.value = 'Antwort ist unterwegs';
    textarea!.dispatchEvent(new Event('input'));
    (fixture.nativeElement as HTMLElement)
      .querySelector<HTMLFormElement>('.chat-form')!
      .dispatchEvent(new Event('submit'));

    expect(webSocket.sendChatMessage).toHaveBeenCalledOnceWith(
      'ABCD12',
      'player-1',
      'Antwort ist unterwegs',
    );

    realtimeEvents.next({
      type: 'CHAT_MESSAGE_SENT',
      payload: {
        messageId: 'message-2',
        playerId: 'player-1',
        senderNickname: 'Marta',
        body: 'Gesendet',
        createdAt: '2026-06-24T10:16:30Z',
      },
    });
    fixture.detectChanges();

    expect(textContent()).toContain('Gesendet');
  });

  it('should unsubscribe from room updates without disconnecting the shared websocket', () => {
    const unsubscribeSpy = jasmine.createSpy('unsubscribe');
    webSocket.subscribeToRoom.and.returnValue(
      new Observable<RealtimeEvent>(() => () => unsubscribeSpy()),
    );

    createComponent();
    fixture.destroy();

    expect(unsubscribeSpy).toHaveBeenCalled();
    expect(webSocket.disconnect).not.toHaveBeenCalled();
  });

  function compiledAssignedCardText(fixture: ComponentFixture<Game>): string {
    return (
      (fixture.nativeElement as HTMLElement).querySelector('.assigned-card-summary')?.textContent ??
      ''
    );
  }
});
