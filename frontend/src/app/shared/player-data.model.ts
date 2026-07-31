export const UNKNOWN_PLAYER_CONST = '' as const;
export const UNKNOWN_ROLE_CONST = '' as const;

export type PlayerData = {
  readonly playerId: string;
  readonly role: PlayerRole | typeof UNKNOWN_ROLE_CONST;
  readonly playerSessionToken: string | null;
};

export type PlayerRole = 'host' | 'player';

export const INITIAL_PLAYER_STATE: PlayerData = {
  playerId: UNKNOWN_PLAYER_CONST,
  role: UNKNOWN_ROLE_CONST,
  playerSessionToken: null,
}
