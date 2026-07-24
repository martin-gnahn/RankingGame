export const PLAYER_JOINED = 'PLAYER_JOINED';
export const PLAYER_LEFT = 'PLAYER_LEFT';
export const PLAYER_REJOINED = 'PLAYER_REJOINED';
export const GAME_STARTED = 'GAME_STARTED';
export const CHAT_MESSAGE_SENT = 'CHAT_MESSAGE_SENT';
export const ANSWER_RANKED = 'ANSWER_RANKED';
export const SORTING_STARTED = 'SORTING_STARTED';
export const EMPTY_EVENT = 'EMPTY_EVENT';

type GameEventType =
  typeof PLAYER_JOINED |
  typeof PLAYER_LEFT |
  typeof PLAYER_REJOINED |
  typeof GAME_STARTED |
  typeof CHAT_MESSAGE_SENT |
  typeof SORTING_STARTED |
  typeof ANSWER_RANKED |
  typeof EMPTY_EVENT;

export interface RealtimeEvent<TPayload = unknown> {
  type: GameEventType;
  payload: TPayload;
}

export type WebSocketConnectionState = 'DISCONNECTED' | 'CONNECTING' | 'CONNECTED';
