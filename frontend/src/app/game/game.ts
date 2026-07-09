import {HttpErrorResponse} from '@angular/common/http';
import {Component, computed, effect, inject, signal, WritableSignal} from '@angular/core';
import {FormBuilder, Validators} from '@angular/forms';
import {toSignal} from '@angular/core/rxjs-interop';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {TranslatePipe, TranslateService} from '@ngx-translate/core';
import {map} from 'rxjs';

import {ChatSidebar} from '../chat-sidebar/chat-sidebar';
import {GameApiService} from '../core/api/game-api.service';
import {RankedAnswerDto} from '../core/api/game.models';
import {RoomApiService} from '../core/api/room-api.service';
import {ActiveRoundResponse, AnswerDto, ChatMessageResponse, RoomCode} from '../core/api/room.models';
import {RealtimeEvent} from '../core/websocket/web-socket.models';
import {WebSocketService} from '../core/websocket/web-socket.service';
import {notBlankValidator} from '../shared/validators/not-blank.validator';
import {AnswerForm} from './answer-form/answer-form';
import {AnswerSubmissionState, RankedAnswerView, ScoreCard} from './game-view.models';
import {Question} from './question/question';
import {RankingAnswer} from './ranking-answer/ranking-answer';
import {RankingOverview} from './ranking-overview/ranking-overview';
import {PlayerSessionStore} from '../shared/player-session-store';

interface ErrorKeyContainer {
  key: string | null;
}

@Component({
  selector: 'app-game',
  imports: [
    RouterLink,
    ChatSidebar,
    TranslatePipe,
    Question,
    AnswerForm,
    RankingAnswer,
    RankingOverview,
  ],
  templateUrl: './game.html',
  styleUrl: './game.scss',
})
export class Game {
  private readonly roomApi = inject(RoomApiService);
  private readonly gameApi = inject(GameApiService);
  private readonly webSocket = inject(WebSocketService);
  private readonly route = inject(ActivatedRoute);
  private readonly formBuilder = inject(FormBuilder);
  protected readonly isCurrentPlayerCaptain = computed(() => this.currentPlayerRole() === 'host');
  private readonly playerSessionStore = inject(PlayerSessionStore);
  protected readonly currentPlayerData = this.playerSessionStore.playerData;
  protected readonly currentPlayerId = this.playerSessionStore.playerId;
  protected readonly currentPlayerRole = this.playerSessionStore.playerRole;
  protected readonly isValidPlayer = this.playerSessionStore.isValidPlayer;

  protected readonly sortingHintKey = computed(() =>
    this.isCurrentPlayerCaptain()
      ? 'game.ranking.hint.captain'
      : 'game.ranking.hint.player',
  );
  private readonly roomCodeParam = toSignal(
    this.route.paramMap.pipe(map((params) => params.get('roomCode'))),
  );

  protected readonly roomCode = computed(() => this.roomCodeParam() ?? '');
  protected readonly activeRound = signal<ActiveRoundResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly submitting = signal(false);
  protected readonly submitted = signal(false);
  protected readonly sortingStarted = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly submitErrorMessage = signal('');
  protected readonly rankingErrorMessage = signal('');
  protected readonly rankingLoading = signal(false);
  protected readonly rankingSubmittingAnswerId = signal<string | null>(null);
  protected readonly chatMessages = signal<ChatMessageResponse[]>([]);
  private readonly translate = inject(TranslateService);
  protected readonly allSubmittedAnswers: WritableSignal<AnswerDto[]> = signal([]);
  protected readonly rankingPositions = signal<RankedAnswerDto[]>([]);
  protected readonly rankedAnswerIds = computed(() =>
    new Set(this.rankingPositions().map((answer) => answer.answerId)),
  );
  protected readonly availableAnswers = computed(() =>
    this.allSubmittedAnswers().filter((answer) => !this.rankedAnswerIds().has(answer.answerId)),
  );
  protected readonly rankedAnswers = computed<RankedAnswerView[]>(() => {
    const submittedAnswersById = new Map(
      this.allSubmittedAnswers().map((answer) => [answer.answerId, answer]),
    );

    return [...this.rankingPositions()]
      .sort((left, right) => left.oneBasedPosition - right.oneBasedPosition)
      .map((ranking) => {
        const submittedAnswer = submittedAnswersById.get(ranking.answerId);

        return {
          ...ranking,
          answerText: submittedAnswer?.answerText ?? ranking.answerText,
          nickname: submittedAnswer?.nickname ?? this.translate.instant('game.unknownPlayer'),
          cardValue: submittedAnswer?.cardValue,
        };
      });
  });
  protected readonly rankingIsComplete = computed(() =>
    this.allSubmittedAnswers().length > 0
    && this.rankedAnswers().length >= this.allSubmittedAnswers().length,
  );
  protected readonly rankingDropDisabled = computed(() =>
    !this.isCurrentPlayerCaptain()
    || this.rankingIsComplete()
    || this.rankingSubmittingAnswerId() !== null,
  );

  protected readonly scoreCards: ScoreCard[] = Array.from({ length: 10 }, (_, index) => {
    const value = index + 1;
    return {
      value,
      tone: value <= 3 ? 'low' : value <= 7 ? 'middle' : 'high',
    };
  });
  protected readonly form = this.formBuilder.nonNullable.group({
    answerText: ['', [Validators.required, notBlankValidator(), Validators.maxLength(500)]],
  });
  protected readonly answerSubmissionState = computed<AnswerSubmissionState>(() => ({
    answerForm: this.form,
    scoreCards: this.scoreCards,
    submitting: this.submitting(),
    submitted: this.submitted(),
    submitErrorMessage: this.submitErrorMessage(),
  }));

  constructor() {
    this.loadActiveRound();

    effect((onCleanup) => {
      const roomCode = this.roomCode();
      const playerId = this.currentPlayerId();

      if (!roomCode) {
        return;
      }

      this.loadChatMessages(roomCode, onCleanup);

      const subscription = this.webSocket.subscribeToRoom(roomCode).subscribe({
        next: (event) => this.handleRealtimeEvent(event),
      });

      if (this.isValidPlayer()) {
        this.webSocket.joinLive(roomCode, playerId);
      }

      onCleanup(() => {
        subscription.unsubscribe();
      });
    });
  }

  protected submitAnswer(): void {
    if (this.submitted()) {
      return;
    }

    const roomCode = this.roomCode();
    const activeRound = this.activeRound();

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    if (!roomCode || !activeRound || !this.isValidPlayer() || this.submitting()) {
      this.submitErrorMessage.set(this.translate.instant('game.errors.submitFailed'));
      return;
    }

    this.submitErrorMessage.set('');
    this.submitting.set(true);

    this.roomApi
      .submitAnswer(roomCode, activeRound.roundId, {
        answerText: this.form.getRawValue().answerText.trim(),
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.submitted.set(true);
          this.form.disable();
        },
        error: (error: unknown) => {
          this.submitting.set(false);
          this.submitErrorMessage.set(this.toErrorMessage(error));
        },
      });
  }

  protected sendChatMessage(body: string): void {
    const roomCode = this.roomCode();
    const playerId = this.currentPlayerId();

    if (!roomCode || !this.isValidPlayer()) {
      return;
    }

    this.webSocket.sendChatMessage(roomCode, playerId, body);
  }

  isAnswerContextValid(roomCode: RoomCode, activeRound: ActiveRoundResponse | null): ErrorKeyContainer {
    if (!roomCode) {
      return {key: 'game.errors.missingRoomCode'};
    }
    if (!activeRound) {
      return {key: 'game.errors.missingRoomCode'};
    }
    if (!this.isValidPlayer()) {
      return {key: 'game.errors.missingPlayerId'};
    }
    if (!this.isCurrentPlayerCaptain() || !roomCode || !activeRound || !this.isValidPlayer()) {
      return {key: 'game.errors.onlyHostCanRank'};
    }
    return {key: null};
  }

  protected rankAnswer(answer: AnswerDto): void {
    const roomCode = this.roomCode();
    const activeRound = this.activeRound();

    const errorKeyContainer = this.isAnswerContextValid(roomCode, activeRound);
    if (errorKeyContainer.key) {
      this.rankingErrorMessage.set(this.translate.instant(errorKeyContainer.key));
      return;
    }

    if (this.rankedAnswerIds().has(answer.answerId) || this.rankingSubmittingAnswerId()) {
      return;
    }

    this.rankingErrorMessage.set('');
    this.rankingSubmittingAnswerId.set(answer.answerId);

    this.gameApi
      .addRankingPosition(roomCode, activeRound!.roundId, {
        answerId: answer.answerId,
      })
      .subscribe({
        next: () => {
          this.rankingSubmittingAnswerId.set(null);
          this.refreshRankingPositions();
        },
        error: (error: unknown) => {
          this.rankingSubmittingAnswerId.set(null);
          this.rankingErrorMessage.set(this.toErrorMessage(error));
        },
      });
  }

  private loadChatMessages(
    roomCode: string,
    onCleanup?: (cleanupFn: () => void) => void,
  ): void {
    const subscription = this.roomApi.getRecentChatMessages(roomCode).subscribe({
      next: (messages) => this.chatMessages.set(messages),
    });

    onCleanup?.(() => subscription.unsubscribe());
  }

  private loadActiveRound(): void {
    const roomCode = this.roomCode();

    if (!roomCode) {
      this.errorMessage.set(this.translate.instant('game.errors.missingRoomCode'));
      return;
    }

    if (!this.isValidPlayer()) {
      this.errorMessage.set(this.translate.instant('game.errors.missingPlayerId'));
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');

    this.roomApi.getActiveRound(roomCode).subscribe({
      next: (activeRound) => {
        // TODO: how to solve this.
        // this.isCurrentPlayerCaptain.set(activeRound.currentPlayerIsCaptain);
        this.activeRound.set(activeRound);
        this.sortingStarted.set(false);
        this.loading.set(false);
        const alreadyHasSubmitted = activeRound.currentPlayerSubmitted;
        this.submitted.set(alreadyHasSubmitted);
        if (alreadyHasSubmitted) {
          this.form.controls.answerText.setValue(activeRound.submittedAnswerByPlayer ?? '');
          this.form.disable();
        }
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.errorMessage.set(this.toErrorMessage(error));
      },
    });
  }

  private loadSubmittedAnswers(): void {
    const roomCode = this.roomCode();
    const activeRound = this.activeRound();
    if (!this.sortingStarted() || !roomCode || !activeRound || !this.isValidPlayer()) {
      return;
    }

    this.gameApi.getSubmittedAnswers(roomCode, activeRound.roundId)
      .subscribe({
        next: (answerResponse) => {
          this.allSubmittedAnswers.set(answerResponse.answers);
        },
        error: (error: unknown) => {
          this.rankingErrorMessage.set(this.toErrorMessage(error));
        },
      });
  }

  private refreshRankingPositions(): void {
    const roomCode = this.roomCode();
    const activeRound = this.activeRound();
    if (!this.sortingStarted() || !roomCode || !activeRound || !this.isValidPlayer()) {
      return;
    }

    this.rankingLoading.set(true);
    this.gameApi.getRankingPositions(roomCode, activeRound.roundId)
      .subscribe({
        next: (response) => {
          const rankedAnswers = response.rankings;
          this.rankingPositions.set(rankedAnswers);
          this.rankingLoading.set(false);
        },
        error: (error: unknown) => {
          this.rankingLoading.set(false);
          this.rankingErrorMessage.set(this.toErrorMessage(error));
        },
      });
  }

  private toErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string') {
      return error.error.message;
    }

    if (error instanceof HttpErrorResponse && error.status === 0) {
      return this.translate.instant('game.errors.serverUnavailable');
    }

    return this.translate.instant('game.errors.roundLoadFailed');
  }

  private isChatMessage(payload: unknown): payload is ChatMessageResponse {
    if (!payload || typeof payload !== 'object') {
      return false;
    }

    const message = payload as Partial<ChatMessageResponse>;
    return typeof message.messageId === 'string'
      && typeof message.playerId === 'string'
      && typeof message.senderNickname === 'string'
      && typeof message.body === 'string'
      && typeof message.createdAt === 'string';
  }

  private isSortingStartedPayload(payload: unknown): payload is { roundId: string } {
    if (!payload || typeof payload !== 'object') {
      return false;
    }

    return typeof (payload as { roundId?: unknown }).roundId === 'string';
  }

  private isEventForActiveRound(payload: unknown): boolean {
    if (!this.isSortingStartedPayload(payload)) {
      return true;
    }

    return this.activeRound()?.roundId === payload.roundId;
  }

  private enterSortingMode(): void {
    this.sortingStarted.set(true);
    this.submitted.set(true);
    this.loadSubmittedAnswers();
    this.refreshRankingPositions();
    this.form.disable();
  }

  private handleRealtimeEvent(event: RealtimeEvent): void {
    const payload = event.payload;
    if (event.type === 'CHAT_MESSAGE_SENT' && this.isChatMessage(payload)) {
      this.chatMessages.update((messages) => [...messages, payload]);
      return;
    }

    // TODO: extract to dedicated method and maybe dedicated service. The realtime event handling.
    // To make this service here thinner.
    if (event.type === 'SORTING_STARTED' && this.isSortingStartedPayload(payload)) {
      if (!this.isEventForActiveRound(payload)) {
        return;
      }

      this.enterSortingMode();
      return;
    }

    if (event.type === 'ANSWER_RANKED' && this.isEventForActiveRound(payload)) {
      this.refreshRankingPositions();
    }
  }
}
