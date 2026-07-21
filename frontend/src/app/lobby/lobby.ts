import {HttpErrorResponse} from '@angular/common/http';
import {Component, computed, effect, inject, signal} from '@angular/core';
import {toSignal} from '@angular/core/rxjs-interop';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {TranslatePipe, TranslateService} from '@ngx-translate/core';
import {map, Subscription} from 'rxjs';

import {ChatSidebar} from '../chat-sidebar/chat-sidebar';
import {RoomApiService} from '../core/api/room-api.service';
import {ChatMessageResponse, RoomResponse} from '../core/api/room.models';
import {
  CHAT_MESSAGE_SENT,
  GAME_STARTED,
  PLAYER_JOINED,
  PLAYER_LEFT,
  PLAYER_REJOINED,
  RealtimeEvent
} from '../core/websocket/web-socket.models';
import {WebSocketService} from '../core/websocket/web-socket.service';
import {UNKNOWN_PLAYER_CONST, UNKNOWN_ROLE_CONST} from '../shared/player-data.model';
import {PlayerSessionStore} from '../shared/player-session-store';

@Component({
  selector: 'app-lobby',
  imports: [RouterLink, ChatSidebar, TranslatePipe],
  templateUrl: './lobby.html',
  styleUrl: './lobby.scss',
})
export class Lobby {
  private readonly roomApi = inject(RoomApiService);
  private readonly webSocket = inject(WebSocketService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);
  // TODO: maybe extract room info to another signal.
  protected readonly isCurrentPlayerHost = computed(() => {
    const room = this.room();
    if (!room) {
      return this.currentPlayerRole() === 'host';
    }
    return room.players.some((player) => player?.playerId === this.currentPlayerId() && player.host && this.currentPlayerRole() === 'host');
  });
  private readonly playerSessionStore = inject(PlayerSessionStore);
  protected readonly currentPlayerData = this.playerSessionStore.playerData;
  protected readonly currentPlayerId = this.playerSessionStore.playerId;
  protected readonly currentPlayerRole = this.playerSessionStore.playerRole;
  protected readonly isValidPlayer = this.playerSessionStore.isValidPlayer;

  private readonly roomCodeParam = toSignal(
    this.route.paramMap.pipe(map((params) => params.get('roomCode'))),
  );

  protected readonly roomCode = computed(() => this.roomCodeParam() ?? '');
  protected readonly room = signal<RoomResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly gameIsInStartingProcess = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly refreshErrorMessage = signal('');
  protected readonly startErrorMessage = signal('');
  protected readonly chatMessages = signal<ChatMessageResponse[]>([]);
  protected readonly playerCount = computed(() => this.room()?.players.length ?? 0);
  protected readonly canStartGame = computed(() => {
    const room = this.room();

    if (!room || room.status !== 'LOBBY' || !this.isCurrentPlayerHost()) {
      return false;
    }

    return room.canStartGame;
  });

  constructor() {
    effect((onCleanup) => {
      const roomCode = this.roomCode();

      if (!roomCode) {
        this.room.set(null);
        this.errorMessage.set(this.translate.instant('lobby.errors.missingRoomCode'));
        this.loading.set(false);
        return;
      }

      this.loadRoom(roomCode, onCleanup);
      this.loadChatMessages(roomCode, onCleanup);
    });

    effect((onCleanup) => {
      const roomCode = this.roomCode();

      if (!roomCode) {
        return;
      }

      const subscription = this.webSocket.subscribeToRoom(roomCode).subscribe({
        next: (event) => this.handleRealtimeEvent(roomCode, event),
        error: () =>
          this.refreshErrorMessage.set(this.translate.instant('lobby.errors.liveUpdateReadFailed')),
      });

      if (this.isValidPlayer()) {
        this.webSocket.joinLive(roomCode, this.currentPlayerId());
      }

      onCleanup(() => {
        subscription.unsubscribe();
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

    if (!roomCode || !this.isValidPlayer() || this.gameIsInStartingProcess()) {
      this.startErrorMessage.set(this.translate.instant('lobby.errors.startFailed'));
      return;
    }

    this.startErrorMessage.set('');
    this.gameIsInStartingProcess.set(true);

    this.roomApi.startRankingGame(roomCode).subscribe({
      next: () => {
        this.gameIsInStartingProcess.set(false);
        this.navigateToGame(roomCode);
      },
      error: (error: unknown) => {
        this.gameIsInStartingProcess.set(false);
        this.startErrorMessage.set(this.toErrorMessage(error));
      },
    });
  }

  protected sendChatMessage(body: string): void {
    const roomCode = this.roomCode();

    if (!roomCode || !this.isValidPlayer()) {
      return;
    }

    this.webSocket.sendChatMessage(roomCode, this.currentPlayerId(), body);
  }

  protected statusLabelKey(status: string): string {
    const labels: Record<string, string> = {
      LOBBY: 'lobby.status.lobby',
      IN_GAME: 'lobby.status.inGame',
      FINISHED: 'lobby.status.finished',
      CLOSED: 'lobby.status.closed',
    };

    return labels[status] ?? status;
  }

  protected connectionLabelKey(status: string): string {
    return status === 'CONNECTED' ? 'lobby.connection.online' : 'lobby.connection.disconnected';
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
          this.refreshErrorMessage.set(this.translate.instant('lobby.errors.liveUpdateLoadFailed'));
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
      error: () => this.refreshErrorMessage.set(this.translate.instant('lobby.errors.chatLoadFailed')),
    });

    onCleanup?.(() => subscription.unsubscribe());
  }

  private handleRealtimeEvent(roomCode: string, event: RealtimeEvent): void {
    if (
      event.type === PLAYER_JOINED ||
      event.type === PLAYER_LEFT ||
      event.type === PLAYER_REJOINED
    ) {
      this.refreshRoom(roomCode);
      return;
    }

    if (event.type === GAME_STARTED) {
      this.navigateToGame(roomCode);
      return;
    }

    const payload = event.payload;
    if (event.type === CHAT_MESSAGE_SENT && this.isChatMessage(payload)) {
      this.chatMessages.update((messages) => [...messages, payload]);
    }
  }

  private navigateToGame(roomCode: string): void {
    const currentPlayerData = this.currentPlayerData();
    if (currentPlayerData.playerId === UNKNOWN_PLAYER_CONST || currentPlayerData.role === UNKNOWN_ROLE_CONST) {
      void this.router.navigate(['/error']);
      return;
    }

    const playerId = currentPlayerData.playerId;
    const role = currentPlayerData.role;
    this.playerSessionStore.storePlayerData({
      playerId: playerId,
      role: role,
      playerSessionToken: currentPlayerData.playerSessionToken,
    });

    void this.router.navigate(['/game', roomCode]);
  }

  private toErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string') {
      return error.error.message;
    }

    if (error instanceof HttpErrorResponse && error.status === 404) {
      return this.translate.instant('lobby.errors.roomNotFound');
    }

    if (error instanceof HttpErrorResponse && error.status === 0) {
      return this.translate.instant('lobby.errors.serverUnavailable');
    }

    return this.translate.instant('lobby.errors.loadFailed');
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
