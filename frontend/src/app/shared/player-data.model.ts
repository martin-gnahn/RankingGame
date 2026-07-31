export const UNKNOWN_PLAYER_CONST = 'UnknownPlayer' as const;
export const UNKNOWN_ROLE_CONST = 'UnknownRole' as const;
export const UNKNOWN_TOKEN_CONST = 'UnknownToken' as const;

type UnknownPlayer = typeof UNKNOWN_PLAYER_CONST;
type UnknownRole = typeof UNKNOWN_ROLE_CONST;
type UnknownToken = typeof UNKNOWN_TOKEN_CONST;

export type PlayerData = {
  readonly playerId: string | null;
  readonly role: PlayerRole | null;
  readonly playerSessionToken: string | null;
};

export type PlayerRole = 'host' | 'player';

export const UNKNOWN_PLAYER_STATE: PlayerData = {
  playerId: null,
  role: null,
  playerSessionToken: null,
}
