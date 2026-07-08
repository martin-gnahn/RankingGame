import {Component, computed, input, output, signal} from '@angular/core';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {TranslatePipe} from '@ngx-translate/core';

import {ChatMessageResponse} from '../core/api/room.models';

@Component({
  selector: 'app-chat-sidebar',
  imports: [ReactiveFormsModule, TranslatePipe],
  templateUrl: './chat-sidebar.html',
  styleUrl: './chat-sidebar.scss',
})
export class ChatSidebar {
  readonly messages = input<ChatMessageResponse[]>([]);
  readonly currentPlayerId = input('');
  readonly disabled = input(false);
  readonly sendMessage = output<string>();

  protected readonly isCollapsed = signal(false);
  protected readonly hasMessages = computed(() => this.messages().length > 0);
  protected readonly form = new FormBuilder().nonNullable.group({
    body: ['', [Validators.required, Validators.maxLength(500)]],
  });

  protected toggleCollapsed(): void {
    this.isCollapsed.update((isCollapsed) => !isCollapsed);
  }

  protected submit(): void {
    if (this.disabled()) {
      return;
    }

    const body = this.form.getRawValue().body.trim();
    if (!body) {
      this.form.markAllAsTouched();
      return;
    }

    if (body.length > 500) {
      this.form.controls.body.setErrors({ maxlength: true });
      this.form.markAllAsTouched();
      return;
    }

    this.sendMessage.emit(body);
    this.form.reset();
  }

  protected isOwnMessage(message: ChatMessageResponse): boolean {
    return message.playerId === this.currentPlayerId();
  }

  protected messageTime(message: ChatMessageResponse): string {
    return new Intl.DateTimeFormat('de-DE', {
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date(message.createdAt));
  }
}
