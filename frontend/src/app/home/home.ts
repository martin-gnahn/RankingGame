import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { finalize } from 'rxjs';

import { RoomActionResponse, RoomApiService } from '../core/api/room-api.service';

@Component({
  selector: 'app-home',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home {
  private readonly formBuilder = inject(FormBuilder);
  private readonly roomApi = inject(RoomApiService);
  private readonly router = inject(Router);

  protected readonly createRoomForm = this.formBuilder.nonNullable.group({
    playerName: ['', [Validators.required, Validators.maxLength(32)]],
  });

  protected readonly joinRoomForm = this.formBuilder.nonNullable.group({
    roomCode: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(8)]],
    playerName: ['', [Validators.required, Validators.maxLength(32)]],
  });

  protected pendingAction: 'create' | 'join' | null = null;
  protected errorMessage = '';

  protected createRoom(): void {
    if (this.createRoomForm.invalid || this.pendingAction) {
      this.createRoomForm.markAllAsTouched();
      return;
    }

    this.errorMessage = '';
    this.pendingAction = 'create';

    const { playerName } = this.createRoomForm.getRawValue();
    this.roomApi
      .createRoom({ playerName: playerName.trim() })
      .pipe(finalize(() => (this.pendingAction = null)))
      .subscribe({
        next: (response) => this.navigateToLobby(response, 'host'),
        error: (error: unknown) => this.showError(error),
      });
  }

  protected joinRoom(): void {
    if (this.joinRoomForm.invalid || this.pendingAction) {
      this.joinRoomForm.markAllAsTouched();
      return;
    }

    this.errorMessage = '';
    this.pendingAction = 'join';

    const { roomCode, playerName } = this.joinRoomForm.getRawValue();
    const normalizedRoomCode = this.normalizeRoomCode(roomCode);

    this.roomApi
      .joinRoom(normalizedRoomCode, { playerName: playerName.trim() })
      .pipe(finalize(() => (this.pendingAction = null)))
      .subscribe({
        next: (response) => this.navigateToLobby({ ...response, roomCode: normalizedRoomCode }, 'player'),
        error: (error: unknown) => this.showError(error),
      });
  }

  protected normalizeJoinCode(): void {
    const roomCode = this.joinRoomForm.controls.roomCode.value;
    this.joinRoomForm.controls.roomCode.setValue(this.normalizeRoomCode(roomCode), { emitEvent: false });
  }

  private normalizeRoomCode(roomCode: string): string {
    return roomCode.trim().toUpperCase();
  }

  private navigateToLobby(response: RoomActionResponse, role: 'host' | 'player'): void {
    const roomCode = response.roomCode ?? response.code ?? response.room?.roomCode ?? response.room?.code;

    if (!roomCode) {
      this.errorMessage = 'Die Raumantwort enthielt keinen Raumcode.';
      return;
    }

    const playerId = response.playerId ?? response.hostPlayerId ?? response.player?.id;
    const queryParams = playerId ? { playerId, role } : { role };

    void this.router.navigate(['/lobby', roomCode], { queryParams });
  }

  private showError(error: unknown): void {
    if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string') {
      this.errorMessage = error.error.message;
      return;
    }

    if (error instanceof HttpErrorResponse && error.status === 0) {
      this.errorMessage = 'Der Server ist gerade nicht erreichbar.';
      return;
    }

    this.errorMessage = 'Die Raumaktion konnte nicht abgeschlossen werden.';
  }
}
