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
  roomId?: string;
  playerId?: string;
  playerName?: string;
  host?: boolean;
}

export type RoomStatus = 'LOBBY' | 'IN_GAME' | 'FINISHED' | 'CLOSED';
export type PlayerConnectionStatus = 'CONNECTED' | 'DISCONNECTED';

export interface RoomPlayerResponse {
  playerId: string;
  nickname: string;
  host: boolean;
  connectionStatus: PlayerConnectionStatus;
}

export interface RoomResponse {
  roomId: string;
  roomCode: RoomCode;
  status: RoomStatus;
  players: RoomPlayerResponse[];
}
