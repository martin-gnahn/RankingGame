import {HttpErrorResponse} from '@angular/common/http';
import {Component, computed, effect, inject, signal} from '@angular/core';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {toSignal} from '@angular/core/rxjs-interop';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {map} from 'rxjs';

import {ChatSidebar} from '../chat-sidebar/chat-sidebar';
import {GameApiService} from '../core/api/game-api.service';
import {RoomApiService} from '../core/api/room-api.service';
import {ActiveRoundResponse, ChatMessageResponse} from '../core/api/room.models';
import {RealtimeEvent} from '../core/websocket/web-socket.models';
import {WebSocketService} from '../core/websocket/web-socket.service';
import {notBlankValidator} from '../shared/validators/not-blank.validator';

interface ScoreCard {
  value: number;
  tone: 'low' | 'middle' | 'high';
}

@Component({
  selector: 'app-game',
  imports: [ReactiveFormsModule, RouterLink, ChatSidebar],
  templateUrl: './game.html',
  styleUrl: './game.scss',
})
export class Game {
  private readonly roomApi = inject(RoomApiService);
  private readonly gameApi = inject(GameApiService);
  private readonly webSocket = inject(WebSocketService);
  private readonly route = inject(ActivatedRoute);
  private readonly formBuilder = inject(FormBuilder);
  private readonly roomCodeParam = toSignal(
    this.route.paramMap.pipe(map((params) => params.get('roomCode'))),
  );
  private readonly queryParamMap = toSignal(this.route.queryParamMap);

  protected readonly roomCode = computed(() => this.roomCodeParam() ?? '');
  protected readonly currentPlayerId = computed(() => this.queryParamMap()?.get('playerId') ?? '');
  protected readonly activeRound = signal<ActiveRoundResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly submitting = signal(false);
  protected readonly submitted = signal(false);
  protected readonly sortingStarted = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly submitErrorMessage = signal('');
  protected readonly chatMessages = signal<ChatMessageResponse[]>([]);
  protected readonly isCurrentPlayerCaptain = computed(() => this.queryParamMap()?.get('role') === 'host');
  protected readonly sortingHintMessage = computed(() =>
    this.isCurrentPlayerCaptain()
      ? 'Alle Antworten wurden abgegeben. Du bist dran: Sortiere jetzt die Antworten.'
      : 'Alle Antworten wurden abgegeben. Der Kapitän sortiert jetzt...',
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

  constructor() {
    setInterval(() => {
      const roomCodeCurrent = this.roomCode();
      this.gameApi.getActivePlayers(roomCodeCurrent).subscribe();
    }, 1000);

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

      if (playerId) {
        this.webSocket.joinLive(roomCode, playerId);
      }

      onCleanup(() => {
        subscription.unsubscribe();
      });
    });
  }

  protected isAssignedCard(assignedCardValue: number, cardValue: number): boolean {
    return Number(assignedCardValue) === cardValue;
  }

  protected submitAnswer(): void {
    if (this.submitted()) {
      return;
    }

    const roomCode = this.roomCode();
    const activeRound = this.activeRound();
    const playerId = this.currentPlayerId();

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    if (!roomCode || !activeRound || !playerId || this.submitting()) {
      this.submitErrorMessage.set('Antwort konnte nicht gesendet werden.');
      return;
    }

    this.submitErrorMessage.set('');
    this.submitting.set(true);

    this.roomApi
      .submitAnswer(roomCode, activeRound.roundId, {
        playerId,
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

    if (!roomCode || !playerId) {
      return;
    }

    this.webSocket.sendChatMessage(roomCode, playerId, body);
  }

  private loadActiveRound(): void {
    const roomCode = this.roomCode();

    if (!roomCode) {
      this.errorMessage.set('Der Raumcode fehlt.');
      return;
    }

    const playerId = this.currentPlayerId();
    if (!playerId) {
      this.errorMessage.set('Die Spieler-ID fehlt.');
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');

    this.roomApi.getActiveRound(roomCode, playerId).subscribe({
      next: (activeRound) => {
        this.activeRound.set(activeRound);
        this.sortingStarted.set(false);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.errorMessage.set(this.toErrorMessage(error));
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

  private handleRealtimeEvent(event: RealtimeEvent): void {
    const payload = event.payload;
    if (event.type === 'CHAT_MESSAGE_SENT' && this.isChatMessage(payload)) {
      this.chatMessages.update((messages) => [...messages, payload]);
      return;
    }

    if (event.type === 'SORTING_STARTED' && this.isSortingStartedPayload(payload)) {
      const activeRound = this.activeRound();
      if (!activeRound || payload.roundId !== activeRound.roundId) {
        return;
      }

      this.sortingStarted.set(true);
      this.submitted.set(true);
      this.form.disable();
    }
  }

  private toErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string') {
      return error.error.message;
    }

    if (error instanceof HttpErrorResponse && error.status === 0) {
      return 'Der Server ist gerade nicht erreichbar.';
    }

    return 'Die Runde konnte nicht geladen werden.';
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

}
