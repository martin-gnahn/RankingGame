import {HttpErrorResponse} from '@angular/common/http';
import {Component, inject} from '@angular/core';
import {Router} from '@angular/router';
import {TranslatePipe, TranslateService} from '@ngx-translate/core';
import {finalize} from 'rxjs';

import {RoomApiService} from '../core/api/room-api.service';
import {CreateRoomRequest, RoomActionResponse} from '../core/api/room.models';
import {CreateRoom} from './create-room/create-room';
import {JoinRoom, JoinRoomRequestPayload} from './join-room/join-room';

@Component({
  selector: 'app-home',
  imports: [CreateRoom, JoinRoom, TranslatePipe],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home {
  private readonly roomApi = inject(RoomApiService);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);

  protected pendingAction: 'create' | 'join' | null = null;
  protected errorMessage = '';

  protected createRoom(request: CreateRoomRequest): void {
    if (this.pendingAction) {
      return;
    }

    this.errorMessage = '';
    this.pendingAction = 'create';

    this.roomApi
      .createRoom(request)
      .pipe(finalize(() => (this.pendingAction = null)))
      .subscribe({
        next: (response) => this.navigateToLobby(response, 'host'),
        error: (error: unknown) => this.showError(error),
      });
  }

  protected joinRoom({ roomCode, playerName }: JoinRoomRequestPayload): void {
    if (this.pendingAction) {
      return;
    }

    this.errorMessage = '';
    this.pendingAction = 'join';

    this.roomApi
      .joinRoom(roomCode, { playerName: playerName.trim() })
      .pipe(finalize(() => (this.pendingAction = null)))
      .subscribe({
        next: (response) => this.navigateToLobby(response, 'player'),
        error: (error: unknown) => this.showError(error),
      });
  }

  private navigateToLobby(response: RoomActionResponse, role: 'host' | 'player'): void {
    if (!response.roomCode) {
      this.errorMessage = this.translate.instant('home.errors.missingRoomCode');
      return;
    }

    const queryParams = response.playerId ? { playerId: response.playerId, role } : { role };

    void this.router.navigate(['/lobby', response.roomCode], { queryParams });
  }

  private showError(error: unknown): void {
    if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string') {
      this.errorMessage = error.error.message;
      return;
    }

    if (error instanceof HttpErrorResponse && error.status === 0) {
      this.errorMessage = this.translate.instant('home.errors.serverUnavailable');
      return;
    }

    this.errorMessage = this.translate.instant('home.errors.roomActionFailed');
  }
}
