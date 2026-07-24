import {HttpErrorResponse, provideHttpClient} from '@angular/common/http';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ActivatedRoute, convertToParamMap, provideRouter} from '@angular/router';
import {BehaviorSubject, Observable, of, Subject, throwError} from 'rxjs';

import {GameApiService} from '../core/api/game-api.service';
import {RoomApiService} from '../core/api/room-api.service';
import {ActiveRoundResponse} from '../core/api/room.models';
import {provideTestingTranslations} from '../core/i18n/translate-testing.providers';
import {RealtimeEvent} from '../core/websocket/web-socket.models';
import {WebSocketService} from '../core/websocket/web-socket.service';
import {PlayerSessionStore} from '../shared/player-session-store';
import {Game} from './game';

describe('Game', () => {
  let fixture: ComponentFixture<Game>;
  let roomApi: jasmine.SpyObj<RoomApiService>;
  let gameApi: jasmine.SpyObj<GameApiService>;
  let webSocket: jasmine.SpyObj<WebSocketService>;
  let playerSessionStore: PlayerSessionStore;
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
    currentPlayerSubmitted: false,
    currentPlayerIsCaptain: false,
  };
  const submittedAnswers = [
    {
      answerId: 'answer-1',
      playerId: 'player-1',
      nickname: 'Marta',
      answerText: 'Mit WLAN-Problemen.',
      cardValue: 7 as const,
    },
    {
      answerId: 'answer-2',
      playerId: 'player-2',
      nickname: 'Alex',
      answerText: 'Im Aufzug stecken geblieben.',
      cardValue: 4 as const,
    },
  ];

  beforeEach(async () => {
    sessionStorage.clear();

    roomApi = jasmine.createSpyObj<RoomApiService>('RoomApiService', [
      'getActiveRound',
      'getRecentChatMessages',
      'submitAnswer',
    ]);
    gameApi = jasmine.createSpyObj<GameApiService>('GameApiService', [
      'addRankingPosition',
      'getActivePlayers',
      'getRankingPositions',
      'getSubmittedAnswers',
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
    gameApi.addRankingPosition.and.returnValue(of({
      roundId: 'round-1',
      playerId: 'player-1',
      answerId: 'answer-1',
      rankingId: 'ranking-1',
      oneBasedPosition: 1,
    }));
    gameApi.getActivePlayers.and.returnValue(of([]));
    gameApi.getRankingPositions.and.returnValue(of({rankings: []}));
    gameApi.getSubmittedAnswers.and.returnValue(of({answers: []}));
    paramMap = new BehaviorSubject(convertToParamMap({roomCode: 'ABCD12'}));
    queryParamMap = new BehaviorSubject(convertToParamMap({playerId: 'player-1'}));

    await TestBed.configureTestingModule({
      imports: [Game],
      providers: [
        provideHttpClient(),
        provideTestingTranslations(),
        provideRouter([]),
        {provide: RoomApiService, useValue: roomApi},
        {provide: GameApiService, useValue: gameApi},
        {provide: WebSocketService, useValue: webSocket},
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: paramMap.asObservable(),
            queryParamMap: queryParamMap.asObservable(),
          },
        },
      ],
    }).compileComponents();

    playerSessionStore = TestBed.inject(PlayerSessionStore);
    storePlayerSession('player-1', 'player');
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  function createComponent(roundResponse: ActiveRoundResponse = activeRound): void {
    roomApi.getActiveRound.and.returnValue(of(roundResponse));
    roomApi.submitAnswer.and.returnValue(
      of({answerId: 'answer-1', roundId: 'round-1', playerId: 'player-1', submitted: true}),
    );
    fixture = TestBed.createComponent(Game);
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

  it('should render the active round question', () => {
    createComponent();

    expect(roomApi.getActiveRound).toHaveBeenCalledOnceWith('ABCD12');
    expect(roomApi.getRecentChatMessages).toHaveBeenCalledOnceWith('ABCD12');
    expect(webSocket.subscribeToRoom).toHaveBeenCalledOnceWith('ABCD12');
    expect(webSocket.joinLive).toHaveBeenCalledOnceWith('ABCD12');
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
      answerText: 'Mit WLAN-Problemen.',
    });
    expect(textContent()).toContain('Antwort gespeichert');

    compiled.querySelector('form')!.dispatchEvent(new Event('submit'));
    expect(roomApi.submitAnswer).toHaveBeenCalledTimes(1);
  });

  it('should show the sorting hint when sorting starts for the active round', () => {
    createComponent();

    realtimeEvents.next({
      type: 'SORTING_STARTED',
      payload: {roundId: 'round-1'},
    });
    fixture.detectChanges();

    expect(textContent()).toContain('Alle Antworten wurden abgegeben. Der Kapitän sortiert jetzt...');
    expect(textContent()).not.toContain('Warte auf die anderen Spieler.');
    expect((fixture.nativeElement as HTMLElement).querySelector<HTMLTextAreaElement>('#answerText')?.disabled)
      .toBeTrue();
  });

  it('should show the captain sorting hint for the host player', () => {
    storePlayerSession('player-1', 'host');
    createComponent({...activeRound, currentPlayerIsCaptain: true});

    realtimeEvents.next({
      type: 'SORTING_STARTED',
      payload: {roundId: 'round-1'},
    });
    fixture.detectChanges();

    expect(textContent()).toContain('Alle Antworten wurden abgegeben. Du bist dran: Sortiere jetzt die Antworten.');
  });

  it('should render submitted answers as host ranking cards', () => {
    storePlayerSession('player-1', 'host');
    gameApi.getSubmittedAnswers.and.returnValue(of({answers: submittedAnswers}));
    createComponent({...activeRound, currentPlayerIsCaptain: true});

    realtimeEvents.next({
      type: 'SORTING_STARTED',
      payload: {roundId: 'round-1'},
    });
    fixture.detectChanges();

    expect(gameApi.getSubmittedAnswers).toHaveBeenCalledOnceWith('ABCD12', 'round-1');
    expect(gameApi.getRankingPositions).toHaveBeenCalledOnceWith('ABCD12', 'round-1');
    expect(textContent()).toContain('Mit WLAN-Problemen.');
    expect(textContent()).toContain('Im Aufzug stecken geblieben.');
    expect(textContent()).toContain('Hier ablegen');
    expect(textContent()).toContain('Einordnen');
  });

  it('should add a ranking position when the host chooses an answer', () => {
    storePlayerSession('host-1', 'host');
    gameApi.getSubmittedAnswers.and.returnValue(of({answers: submittedAnswers}));
    gameApi.getRankingPositions.and.returnValues(
      of({rankings: []}),
      of({
        rankings: [
          {
            rankingId: 'ranking-1',
            answerId: 'answer-1',
            playerId: 'player-1',
            answerText: 'Mit WLAN-Problemen.',
            oneBasedPosition: 1,
          },
        ],
      }),
    );
    createComponent({...activeRound, currentPlayerIsCaptain: true});

    realtimeEvents.next({
      type: 'SORTING_STARTED',
      payload: {roundId: 'round-1'},
    });
    fixture.detectChanges();

    const firstRankButton = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.rank-button')!;
    firstRankButton.click();
    fixture.detectChanges();

    expect(gameApi.addRankingPosition).toHaveBeenCalledOnceWith('ABCD12', 'round-1', {
      answerId: 'answer-1',
    });
    expect(gameApi.getRankingPositions).toHaveBeenCalledTimes(2);
    expect(textContent()).toContain('Aktuelles Ranking');
    expect(textContent()).toContain('Marta');
  });

  it('should refresh ranking positions for all players after an answer ranked event', () => {
    gameApi.getSubmittedAnswers.and.returnValue(of({answers: submittedAnswers}));
    gameApi.getRankingPositions.and.returnValues(
      of({rankings: []}),
      of({
        rankings: [
          {
            rankingId: 'ranking-1',
            answerId: 'answer-2',
            playerId: 'player-2',
            answerText: 'Im Aufzug stecken geblieben.',
            oneBasedPosition: 1,
          },
        ],
      }),
    );
    createComponent();

    realtimeEvents.next({
      type: 'SORTING_STARTED',
      payload: {roundId: 'round-1'},
    });
    realtimeEvents.next({
      type: 'ANSWER_RANKED',
      payload: {roundId: 'round-1', answerId: 'answer-2', oneBasedPosition: 1},
    });
    fixture.detectChanges();

    expect(gameApi.getRankingPositions).toHaveBeenCalledTimes(2);
    expect(textContent()).toContain('Aktuelles Ranking');
    expect(textContent()).toContain('Alex');
    expect(textContent()).toContain('Im Aufzug stecken geblieben.');
  });

  it('should ignore sorting started events for a different round', () => {
    createComponent();

    realtimeEvents.next({
      type: 'SORTING_STARTED',
      payload: {roundId: 'round-2'},
    });
    fixture.detectChanges();

    expect(textContent()).not.toContain('Alle Antworten wurden abgegeben.');
    expect((fixture.nativeElement as HTMLElement).querySelector<HTMLTextAreaElement>('#answerText')?.disabled)
      .toBeFalse();
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
      throwError(() => new HttpErrorResponse({status: 400, error: {message: 'No active game is running'}})),
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

    expect(webSocket.sendChatMessage).toHaveBeenCalledOnceWith('ABCD12', 'Antwort ist unterwegs');

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

  it('should collapse and restore the chat sidebar', () => {
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

    const compiled = fixture.nativeElement as HTMLElement;
    const collapseButton = compiled.querySelector<HTMLButtonElement>('.collapse-button')!;

    expect(collapseButton.getAttribute('aria-expanded')).toBe('true');
    expect(compiled.querySelector('.message-list')).not.toBeNull();
    expect(compiled.querySelector('.chat-form')).not.toBeNull();

    collapseButton.click();
    fixture.detectChanges();

    expect(collapseButton.getAttribute('aria-expanded')).toBe('false');
    expect(collapseButton.getAttribute('aria-label')).toBe('Chat öffnen');
    expect(compiled.querySelector('.message-list')).toBeNull();
    expect(compiled.querySelector('.chat-form')).toBeNull();
    expect(textContent()).not.toContain('Ich bin drin');

    collapseButton.click();
    fixture.detectChanges();

    expect(collapseButton.getAttribute('aria-expanded')).toBe('true');
    expect(collapseButton.getAttribute('aria-label')).toBe('Chat minimieren');
    expect(compiled.querySelector('.message-list')).not.toBeNull();
    expect(compiled.querySelector('.chat-form')).not.toBeNull();
    expect(textContent()).toContain('Ich bin drin');
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
