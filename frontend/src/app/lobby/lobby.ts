import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription, map } from 'rxjs';

import { ChatSidebar } from '../chat-sidebar/chat-sidebar';
import { RoomApiService } from '../core/api/room-api.service';
import { ChatMessageResponse, RoomResponse } from '../core/api/room.models';
import { RealtimeEvent } from '../core/websocket/web-socket.models';
import { WebSocketService } from '../core/websocket/web-socket.service';

@Component({
  selector: 'app-lobby',
  imports: [RouterLink, ChatSidebar],
  templateUrl: './lobby.html',
  styleUrl: './lobby.scss',
})
export class Lobby {
  private readonly roomApi = inject(RoomApiService);
  private readonly webSocket = inject(WebSocketService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly roomCodeParam = toSignal(
    this.route.paramMap.pipe(map((params) => params.get('roomCode'))),
  );
  private readonly queryParamMap = toSignal(this.route.queryParamMap);

  protected readonly roomCode = computed(() => this.roomCodeParam() ?? '');
  protected readonly room = signal<RoomResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly startingGame = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly refreshErrorMessage = signal('');
  protected readonly startErrorMessage = signal('');
  protected readonly chatMessages = signal<ChatMessageResponse[]>([]);
  protected readonly currentPlayerId = computed(() => this.queryParamMap()?.get('playerId') ?? '');
  protected readonly isCurrentPlayerHost = computed(() => {
    const room = this.room();
    const currentPlayerId = this.currentPlayerId();

    if (!room) {
      return this.queryParamMap()?.get('role') === 'host';
    }

    return room.players.some((player) => player.playerId === currentPlayerId && player.host);
  });
  protected readonly playerCount = computed(() => this.room()?.players.length ?? 0);
  protected readonly canStartGame = computed(() => {
    const room = this.room();
    const currentPlayerId = this.currentPlayerId();

    if (!room || room.status !== 'LOBBY' || !this.isCurrentPlayerHost()) {
      return false;
    }

    // return room.players.some(
    //   (player) => player.playerId !== currentPlayerId && player.connectionStatus === 'CONNECTED',
    // );
    return true;
  });

  constructor() {
    effect((onCleanup) => {
      const roomCode = this.roomCode();

      if (!roomCode) {
        this.room.set(null);
        this.errorMessage.set('Der Raumcode fehlt.');
        this.loading.set(false);
        return;
      }

      this.loadRoom(roomCode, onCleanup);
      this.loadChatMessages(roomCode, onCleanup);
    });

    effect((onCleanup) => {
      const roomCode = this.roomCode();
      const playerId = this.currentPlayerId();

      if (!roomCode) {
        return;
      }

      const subscription = this.webSocket.subscribeToRoom(roomCode).subscribe({
        next: (event) => this.handleRealtimeEvent(roomCode, event),
        error: () => this.refreshErrorMessage.set('Live-Update konnte nicht gelesen werden.'),
      });

      if (playerId) {
        this.webSocket.joinLive(roomCode, playerId);
      }

      onCleanup(() => {
        subscription.unsubscribe();
        this.webSocket.disconnect();
      });
    });
  }

  protected retryLoad(): void {
    const roomCode = this.roomCode();

    if (roomCode) {
      this.loadRoom(roomCode);
    }
  }

  protected startGame(): void {
    const roomCode = this.roomCode();
    const hostPlayerId = this.currentPlayerId();

    if (!roomCode || !hostPlayerId || this.startingGame()) {
      this.startErrorMessage.set('Das Spiel konnte nicht gestartet werden.');
      return;
    }

    this.startErrorMessage.set('');
    this.startingGame.set(true);

    this.roomApi.startRankingGame(roomCode, { hostPlayerId }).subscribe({
      next: () => {
        this.startingGame.set(false);
        this.navigateToGame(roomCode);
      },
      error: (error: unknown) => {
        this.startingGame.set(false);
        this.startErrorMessage.set(this.toErrorMessage(error));
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

  protected statusLabel(status: string): string {
    const labels: Record<string, string> = {
      LOBBY: 'Wartet auf Spieler',
      IN_GAME: 'Spiel laeuft',
      FINISHED: 'Spiel beendet',
      CLOSED: 'Raum geschlossen',
    };

    return labels[status] ?? status;
  }

  protected connectionLabel(status: string): string {
    return status === 'CONNECTED' ? 'Online' : 'Getrennt';
  }

  private loadRoom(
    roomCode: string,
    onCleanup?: (cleanupFn: () => void) => void,
    options: { showLoading: boolean } = { showLoading: true },
  ): void {
    if (options.showLoading) {
      this.loading.set(true);
      this.errorMessage.set('');
    }

    const subscription: Subscription = this.roomApi.getRoom(roomCode).subscribe({
      next: (room) => {
        this.room.set(room);
        this.loading.set(false);
        this.refreshErrorMessage.set('');
      },
      error: (error: unknown) => {
        if (this.room()) {
          this.refreshErrorMessage.set('Live-Update konnte nicht geladen werden.');
        } else {
          this.room.set(null);
          this.errorMessage.set(this.toErrorMessage(error));
        }
        this.loading.set(false);
      },
    });

    onCleanup?.(() => subscription.unsubscribe());
  }

  private refreshRoom(roomCode: string): void {
    this.loadRoom(roomCode, undefined, { showLoading: false });
  }

  private loadChatMessages(roomCode: string, onCleanup?: (cleanupFn: () => void) => void): void {
    const subscription = this.roomApi.getRecentChatMessages(roomCode).subscribe({
      next: (messages) => this.chatMessages.set(messages),
      error: () => this.refreshErrorMessage.set('Chat konnte nicht geladen werden.'),
    });

    onCleanup?.(() => subscription.unsubscribe());
  }

  private handleRealtimeEvent(roomCode: string, event: RealtimeEvent): void {
    if (
      event.type === 'PLAYER_JOINED' ||
      event.type === 'PLAYER_LEFT'
    ) {
      this.refreshRoom(roomCode);
      return;
    }

    if (event.type === 'GAME_STARTED') {
      this.navigateToGame(roomCode);
      return;
    }

    const payload = event.payload;
    if (event.type === 'CHAT_MESSAGE_SENT' && this.isChatMessage(payload)) {
      this.chatMessages.update((messages) => [...messages, payload]);
    }
  }

  private navigateToGame(roomCode: string): void {
    const queryParams: Record<string, string> = {};
    const playerId = this.currentPlayerId();
    const role = this.queryParamMap()?.get('role') ?? '';

    if (playerId) {
      queryParams['playerId'] = playerId;
    }

    if (role) {
      queryParams['role'] = role;
    }

    void this.router.navigate(['/game', roomCode], { queryParams });
  }

  private toErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string') {
      return error.error.message;
    }

    if (error instanceof HttpErrorResponse && error.status === 404) {
      return 'Dieser Raum wurde nicht gefunden.';
    }

    if (error instanceof HttpErrorResponse && error.status === 0) {
      return 'Der Server ist gerade nicht erreichbar.';
    }

    return 'Die Lobby konnte nicht geladen werden.';
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
}
