export interface RealtimeEvent<TPayload = unknown> {
  type: string;
  payload: TPayload;
}

export type WebSocketConnectionState = 'DISCONNECTED' | 'CONNECTING' | 'CONNECTED';
