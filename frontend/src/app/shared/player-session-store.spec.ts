import {TestBed} from '@angular/core/testing';

import {INITIAL_PLAYER_STATE} from './player-data.model';
import {PlayerSessionStore} from './player-session-store';

describe('PlayerSessionStore', () => {
  let service: PlayerSessionStore;

  beforeEach(() => {
    sessionStorage.clear();

    TestBed.configureTestingModule({
      providers: [PlayerSessionStore],
    });

    service = TestBed.inject(PlayerSessionStore);
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should keep player data in a signal and persist it to session storage', () => {
    const playerData = {
      playerId: 'player-1',
      role: 'host' as const,
      playerSessionToken: 'token-1',
    };

    service.storePlayerData(playerData);

    expect(service.playerData()).toEqual(playerData);
    expect(service.playerSessionToken()).toBe('token-1');
    expect(sessionStorage.getItem('playerData')).toBe(JSON.stringify(playerData));
  });

  it('should clear player data from signal and session storage', () => {
    service.storePlayerData({
      playerId: 'player-1',
      role: 'host',
      playerSessionToken: 'token-1',
    });

    service.clearPlayerData();

    expect(service.playerData()).toEqual(INITIAL_PLAYER_STATE);
    expect(sessionStorage.getItem('playerData')).toBeNull();
  });
});
