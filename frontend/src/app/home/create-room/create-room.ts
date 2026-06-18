import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { CreateRoomRequest } from '../../core/api/room.models';
import { notBlankValidator } from '../../shared/validators/not-blank.validator';

@Component({
  selector: 'app-create-room',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './create-room.html',
  styleUrl: './create-room.scss',
})
export class CreateRoom {
  private readonly formBuilder = inject(FormBuilder);

  @Input() disabled = false;
  @Input() loading = false;
  @Output() createRoomRequested = new EventEmitter<CreateRoomRequest>();

  protected readonly form = this.formBuilder.nonNullable.group({
    playerName: ['Martin', [Validators.required, notBlankValidator(), Validators.maxLength(32)]],
  });

  protected submit(): void {
    if (this.form.invalid || this.disabled) {
      this.form.markAllAsTouched();
      return;
    }

    const { playerName } = this.form.getRawValue();
    this.createRoomRequested.emit({ playerName: playerName.trim() });
  }
}
