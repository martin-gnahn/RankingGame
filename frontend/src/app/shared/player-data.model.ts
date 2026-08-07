export type PlayerData = {
  readonly playerId: string | null;
  readonly role: PlayerRole | null;
  readonly playerSessionToken: string | null;
};

export type PlayerRole = 'host' | 'player';

