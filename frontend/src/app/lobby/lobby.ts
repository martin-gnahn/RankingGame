import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription, map } from 'rxjs';

import { RoomApiService } from '../core/api/room-api.service';
import { RoomResponse } from '../core/api/room.models';

@Component({
  selector: 'app-lobby',
  imports: [RouterLink],
  templateUrl: './lobby.html',
  styleUrl: './lobby.scss',
})
export class Lobby {
  private readonly roomApi = inject(RoomApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly roomCodeParam = toSignal(this.route.paramMap.pipe(map((params) => params.get('roomCode'))));
  private readonly queryParamMap = toSignal(this.route.queryParamMap);

  protected readonly roomCode = computed(() => this.roomCodeParam() ?? '');
  protected readonly room = signal<RoomResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal('');
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
    });
  }

  protected retryLoad(): void {
    const roomCode = this.roomCode();

    if (roomCode) {
      this.loadRoom(roomCode);
    }
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

  private loadRoom(roomCode: string, onCleanup?: (cleanupFn: () => void) => void): void {
    this.loading.set(true);
    this.errorMessage.set('');

    const subscription: Subscription = this.roomApi.getRoom(roomCode).subscribe({
      next: (room) => {
        this.room.set(room);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.room.set(null);
        this.errorMessage.set(this.toErrorMessage(error));
        this.loading.set(false);
      },
    });

    onCleanup?.(() => subscription.unsubscribe());
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
}
