import {Component, EventEmitter, inject, Input, Output} from '@angular/core';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {TranslatePipe} from '@ngx-translate/core';

import {isRoomCode, ROOM_CODE_PATTERN, RoomCode} from '../../core/api/room.models';
import {notBlankValidator} from '../../shared/validators/not-blank.validator';

export interface JoinRoomRequestPayload {
  roomCode: RoomCode;
  playerName: string;
}

@Component({
  selector: 'app-join-room',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    TranslatePipe,
  ],
  templateUrl: './join-room.html',
  styleUrl: './join-room.scss',
})
export class JoinRoom {
  private readonly formBuilder = inject(FormBuilder);

  @Input() disabled = false;
  @Input() loading = false;
  @Output() joinRoomRequested = new EventEmitter<JoinRoomRequestPayload>();

  protected readonly form = this.formBuilder.nonNullable.group({
    roomCode: ['', [Validators.required, Validators.pattern(ROOM_CODE_PATTERN)]],
    playerName: ['Felix', [Validators.required, notBlankValidator(), Validators.maxLength(32)]],
  });

  protected submit(): void {
    if (this.disabled) {
      return;
    }

    this.sanitizeJoinCodeInput();

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { roomCode, playerName } = this.form.getRawValue();

    if (!isRoomCode(roomCode)) {
      this.form.controls.roomCode.setErrors({ roomCode: true });
      this.form.controls.roomCode.markAsTouched();
      return;
    }

    this.joinRoomRequested.emit({ roomCode, playerName: playerName.trim() });
  }

  protected sanitizeJoinCodeInput(): void {
    const roomCode = this.form.controls.roomCode.value;
    this.form.controls.roomCode.setValue(this.normalizeRoomCode(roomCode), { emitEvent: false });
  }

  private normalizeRoomCode(roomCode: string): string {
    return roomCode
      .trim()
      .toUpperCase()
      .replace(/[^A-Z0-9]/g, '')
      .slice(0, 8);
  }
}
