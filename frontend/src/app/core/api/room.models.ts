export const ROOM_CODE_PATTERN = /^[A-Z0-9]{4,8}$/;

export type RoomCode = string;

export function isRoomCode(value: string): value is RoomCode {
  return ROOM_CODE_PATTERN.test(value);
}

export interface CreateRoomRequest {
  playerName: string;
}

export interface JoinRoomRequest {
  playerName: string;
}

export interface RoomActionResponse {
  roomCode: RoomCode;
  playerId?: string;
}
