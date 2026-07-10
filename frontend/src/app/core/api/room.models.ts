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
  playerToken: string;
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
  canStartGame: boolean;
  startBlockedReason?: string | null;
}

export interface StartGameRequest {
  hostPlayerId: string;
}

export interface StartGameResponse {
  roomCode: RoomCode;
}

export interface ActiveRoundResponse {
  roomId: string;
  roomCode: RoomCode;
  gameSessionId: string;
  roundId: string;
  roundNumber: number;
  questionId: string;
  questionText: string;
  assignedCardValue: number;
  currentPlayerSubmitted: boolean;
  currentPlayerIsCaptain: boolean;
}

export interface SubmitAnswerRequest {
  playerId: string;
  answerText: string;
}

export interface SubmitAnswerResponse {
  answerId: string;
  roundId: string;
  playerId: string;
  submitted: boolean;
}

export interface ChatMessageResponse {
  messageId: string;
  playerId: string;
  senderNickname: string;
  body: string;
  createdAt: string;
}

export interface AnswerDto {
  answerId: string;
  playerId: string;
  nickname: string;
  answerText: string;
  cardValue: 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10;
}

export interface AnswerResponseDto {
  answers: AnswerDto[];
}
