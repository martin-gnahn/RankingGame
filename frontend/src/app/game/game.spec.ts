import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { provideRouter } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';

import { RoomApiService } from '../core/api/room-api.service';
import { ActiveRoundResponse } from '../core/api/room.models';
import { Game } from './game';

describe('Game', () => {
  let fixture: ComponentFixture<Game>;
  let roomApi: jasmine.SpyObj<RoomApiService>;
  let paramMap: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let queryParamMap: BehaviorSubject<ReturnType<typeof convertToParamMap>>;

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
      'submitAnswer',
    ]);
    paramMap = new BehaviorSubject(convertToParamMap({ roomCode: 'ABCD12' }));
    queryParamMap = new BehaviorSubject(convertToParamMap({ playerId: 'player-1' }));

    await TestBed.configureTestingModule({
      imports: [Game],
      providers: [
        provideRouter([]),
        { provide: RoomApiService, useValue: roomApi },
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
    expect(textContent()).toContain('Runde 1');
    expect(textContent()).toContain('Welche Ausrede funktioniert immer?');
    expect(textContent()).toContain('Antwort');
    expect(textContent()).toContain('Deine Karte: 7');
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

    fixture = TestBed.createComponent(Game);
    fixture.detectChanges();

    expect(textContent()).toContain('No active game is running');
  });

  function compiledAssignedCardText(fixture: ComponentFixture<Game>): string {
    return (
      (fixture.nativeElement as HTMLElement).querySelector('.assigned-card-summary')?.textContent ??
      ''
    );
  }
});
