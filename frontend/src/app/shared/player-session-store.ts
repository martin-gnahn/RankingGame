import {computed, effect, Injectable, Signal, signal} from '@angular/core';
import {PlayerData} from './player-data.model';

const STORAGE_KEY = 'playerData';

@Injectable({
  providedIn: 'root',
})
export class PlayerSessionStore {
  private readonly storage = sessionStorage;
  readonly playerId = computed(
    () => this.playerData()?.playerId ?? null
  );
  readonly playerRole = computed(
    () => this.playerData()?.role ?? null
  );
  readonly playerSessionToken = computed(
    () => this.playerData()?.playerSessionToken ?? null
  );
  readonly hasValidPlayerId: Signal<boolean> = computed(
    () => {
      const playerId = this.playerId();
      return !!playerId && this.hasText(playerId);
    }
  );

  hasAllData(playerData: PlayerData | null): playerData is PlayerData {
    return !!playerData && !!playerData.playerId && !!playerData.playerSessionToken && !!playerData.role;
  }

  private readonly playerDataInternal = signal<PlayerData | null>(this.loadFromStorage());
  readonly playerData = computed(
    () => this.playerDataInternal()
  );

  constructor() {
    effect(() => {
      const state = this.playerData();

      if (!this.hasAllData(state)) {
        sessionStorage.removeItem(STORAGE_KEY);
        this.clearPlayerData();
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
    this.playerDataInternal.set(null);
    this.storage?.removeItem(STORAGE_KEY);
  }

  private loadFromStorage(): PlayerData | null{
    try {
      const json = sessionStorage.getItem(STORAGE_KEY);

      if (!json) {
        return null;
      }

      const storedValue: unknown = JSON.parse(json);
      if (!this.isPlayerData(storedValue)) {
        sessionStorage.removeItem(STORAGE_KEY);
        return null;
      }

      return storedValue;
    } catch {
      sessionStorage.removeItem(STORAGE_KEY);
      return null;
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

  private hasText(val: string): val is string {
    return !!val && val.trim().length > 0 && typeof val === 'string';
  }
}
