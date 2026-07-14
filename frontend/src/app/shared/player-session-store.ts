import {computed, effect, Injectable, signal} from '@angular/core';
import {PlayerData, UNKNOWN_PLAYER_CONST, UNKNOWN_PLAYER_STATE} from './player-data.model';

const STORAGE_KEY = 'playerData';

@Injectable({
  providedIn: 'root',
})
export class PlayerSessionStore {
  private readonly storage = sessionStorage;
  readonly playerId = computed(
    () => this.playerData().playerId
  );
  readonly playerRole = computed(
    () => this.playerData().role,
  );
  readonly playerSessionToken = computed(
    () => this.playerData().playerSessionToken
  );
  readonly isValidPlayer = computed(
    () => this.hasText(this.playerId()) && this.playerId() !== UNKNOWN_PLAYER_CONST
  );

  readonly hasSession = computed(() => {
    const state = this.playerData();
    return state.playerId !== null && state.role !== null;
  });
  readonly isHost = computed(() => {
    const state = this.playerData();
    return state.role === 'host';
  });
  private readonly playerDataInternal = signal<PlayerData>(this.loadFromStorage());
  // readonly playerData = this.playerDataInternal.asReadonly();
  readonly playerData = computed(
    () => this.playerDataInternal()
  );

  constructor() {
    effect(() => {
      const state = this.playerData();

      if (!state.playerId || !state.role) {
        sessionStorage.removeItem(STORAGE_KEY);
        return;
      }

      sessionStorage.setItem(
        STORAGE_KEY,
        JSON.stringify(state),
      );
    });
  }

  storePlayerData(playerData: PlayerData): void {
    this.playerDataInternal.set(playerData);
    this.storage?.setItem(STORAGE_KEY, JSON.stringify(playerData));
  }

  clearPlayerData(): void {
    this.playerDataInternal.set(UNKNOWN_PLAYER_STATE);
    this.storage?.removeItem(STORAGE_KEY);
  }

  private loadFromStorage(): PlayerData {
    try {
      const json = sessionStorage.getItem(STORAGE_KEY);

      if (!json) {
        return UNKNOWN_PLAYER_STATE;
      }

      const storedValue: unknown = JSON.parse(json);
      if (!this.isPlayerData(storedValue)) {
        sessionStorage.removeItem(STORAGE_KEY);
        return UNKNOWN_PLAYER_STATE;
      }

      return storedValue;
    } catch {
      sessionStorage.removeItem(STORAGE_KEY);
      return UNKNOWN_PLAYER_STATE;
    }
  }

  private isPlayerData(value: unknown): value is PlayerData {
    if (!value || typeof value !== 'object') {
      return false;
    }

    const candidate = value as Partial<PlayerData>;
    return typeof candidate.playerId === 'string'
      && (candidate.role === 'host' || candidate.role === 'player')
      && (
        candidate.playerSessionToken === null ||
        typeof candidate.playerSessionToken === 'string'
      );
  }

  private hasText(val: string): boolean {
    return !!val && val.trim().length > 0;
  }
}
