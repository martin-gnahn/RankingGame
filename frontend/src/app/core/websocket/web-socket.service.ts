import {inject, Injectable, InjectionToken} from '@angular/core';
import {Client, type IMessage, type IPublishParams, type StompConfig, type StompSubscription,} from '@stomp/stompjs';
import {BehaviorSubject, Observable} from 'rxjs';

import {environment} from '../../../environments/environment';
import {RoomCode} from '../api/room.models';
import {EMPTY_EVENT, RealtimeEvent, WebSocketConnectionState} from './web-socket.models';

interface StompClientAdapter {
  active: boolean;
  connected: boolean;
  activate(): void;
  deactivate(): Promise<void>;
  publish(params: IPublishParams): void;
  subscribe(destination: string, callback: (message: IMessage) => void): StompSubscription;
}

export type StompClientFactory = (config: StompConfig) => StompClientAdapter;

export const STOMP_CLIENT_FACTORY = new InjectionToken<StompClientFactory>('STOMP_CLIENT_FACTORY', {
  providedIn: 'root',
  factory: () => (config: StompConfig) => new Client(config),
});

@Injectable({
  providedIn: 'root',
})
export class WebSocketService {
  private readonly clientFactory = inject(STOMP_CLIENT_FACTORY);
  private readonly connectionStateSubject = new BehaviorSubject<WebSocketConnectionState>('DISCONNECTED');
  private readonly pendingSubscriptions = new Set<() => void>();
  private readonly client = this.clientFactory({
    brokerURL: this.resolveBrokerUrl(environment.webSocketUrl),
    reconnectDelay: 5000,
    debug: () => undefined,
    onConnect: () => this.handleConnected(),
    onWebSocketClose: () => this.handleDisconnected(),
    onWebSocketError: () => this.handleDisconnected(),
    onStompError: () => this.handleDisconnected(),
  });

  readonly connectionState$ = this.connectionStateSubject.asObservable();

  connect(): void {
    if (this.client.connected) {
      this.connectionStateSubject.next('CONNECTED');
      return;
    }

    if (!this.client.active) {
      this.connectionStateSubject.next('CONNECTING');
      this.client.activate();
    }
  }

  disconnect(): void {
    this.pendingSubscriptions.clear();
    void this.client.deactivate();
    this.connectionStateSubject.next('DISCONNECTED');
  }

  joinLive(roomCode: RoomCode, playerId: string): void {
    const publishJoinLive = () => {
      this.client.publish({
        destination: `/app/rooms/${roomCode}/join-live`,
        body: JSON.stringify({ playerId }),
      });
    };

    if (this.client.connected) {
      publishJoinLive();
      return;
    }

    this.pendingSubscriptions.add(publishJoinLive);
    this.connect();
  }

  sendChatMessage(roomCode: RoomCode, playerId: string, body: string): void {
    const publishChatMessage = () => {
      this.client.publish({
        destination: `/app/rooms/${roomCode}/chat`,
        body: JSON.stringify({ playerId, body }),
      });
    };

    if (this.client.connected) {
      publishChatMessage();
      return;
    }

    this.pendingSubscriptions.add(publishChatMessage);
    this.connect();
  }

  subscribeToRoom(roomCode: RoomCode): Observable<RealtimeEvent> {
    const destination = `/topic/rooms/${roomCode}`;

    return new Observable<RealtimeEvent>((observer) => {
      let subscription: StompSubscription | null = null;

      const subscribeWhenConnected = () => {
        subscription = this.client.subscribe(destination, (message) => {
          try {
            observer.next(this.parseMessage(message));
          } catch (error) {
            observer.error(error);
          }
        });
      };

      if (this.client.connected) {
        subscribeWhenConnected();
      } else {
        this.pendingSubscriptions.add(subscribeWhenConnected);
        this.connect();
      }

      return () => {
        this.pendingSubscriptions.delete(subscribeWhenConnected);
        subscription?.unsubscribe();
      };
    });
  }

  private handleConnected(): void {
    this.connectionStateSubject.next('CONNECTED');

    for (const subscribe of this.pendingSubscriptions) {
      subscribe();
    }

    this.pendingSubscriptions.clear();
  }

  private handleDisconnected(): void {
    this.connectionStateSubject.next('DISCONNECTED');
  }

  private parseMessage(message: IMessage): RealtimeEvent {
    if (!message.body) {
      return {type: EMPTY_EVENT, payload: null};
    }

    return JSON.parse(message.body) as RealtimeEvent;
  }

  private resolveBrokerUrl(configuredUrl: string): string {
    if (configuredUrl.startsWith('ws://') || configuredUrl.startsWith('wss://')) {
      return configuredUrl;
    }

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    return `${protocol}//${window.location.host}${configuredUrl}`;
  }
}
