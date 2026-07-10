export const UNKNOWN_PLAYER_CONST = 'UnknownPlayer' as const;
export const UNKNOWN_ROLE_CONST = 'UnknownRole' as const;

type UnknownPlayer = typeof UNKNOWN_PLAYER_CONST;
type UnknownRole = typeof UNKNOWN_ROLE_CONST;

export type PlayerData = {
  readonly playerId: string | UnknownPlayer;
  readonly role: PlayerRole | UnknownRole;
  readonly playerSessionToken: string | null;
};

export type PlayerRole = 'host' | 'player';

export const INITIAL_PLAYER_STATE: PlayerData = {
  playerId: UNKNOWN_PLAYER_CONST,
  role: UNKNOWN_ROLE_CONST,
  playerSessionToken: null,
}
