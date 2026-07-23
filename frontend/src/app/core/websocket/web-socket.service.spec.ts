import {TestBed} from '@angular/core/testing';
import {type IMessage, type StompConfig, type StompSubscription} from '@stomp/stompjs';

import {RealtimeEvent} from './web-socket.models';
import {STOMP_CLIENT_FACTORY, WebSocketService} from './web-socket.service';

class FakeStompClient {
  active = false;
  connected = false;
  readonly unsubscribeSpy = jasmine.createSpy('unsubscribe');
  readonly publishedMessages: unknown[] = [];
  lastDestination = '';
  lastCallback: ((message: IMessage) => void) | null = null;

  readonly activate = jasmine.createSpy('activate').and.callFake(() => {
    this.active = true;
  });

  readonly deactivate = jasmine.createSpy('deactivate').and.callFake(() => {
    this.active = false;
    this.connected = false;
    return Promise.resolve();
  });

  readonly publish = jasmine.createSpy('publish').and.callFake((message: unknown) => {
    this.publishedMessages.push(message);
  });

  readonly subscribe = jasmine
    .createSpy('subscribe')
    .and.callFake((destination: string, callback: (message: IMessage) => void): StompSubscription => {
      this.lastDestination = destination;
      this.lastCallback = callback;
      return { id: 'sub-1', unsubscribe: this.unsubscribeSpy } as unknown as StompSubscription;
    });
}

describe('WebSocketService', () => {
  let service: WebSocketService;
  let fakeClient: FakeStompClient;
  let stompConfig: StompConfig;

  beforeEach(() => {
    fakeClient = new FakeStompClient();

    TestBed.configureTestingModule({
      providers: [
        WebSocketService,
        {
          provide: STOMP_CLIENT_FACTORY,
          useValue: (config: StompConfig) => {
            stompConfig = config;
            return fakeClient;
          },
        },
      ],
    });

    service = TestBed.inject(WebSocketService);
  });

  it('should configure the STOMP client for the backend websocket endpoint', () => {
    expect(stompConfig.brokerURL).toBe('ws://localhost:8080/ws');
    expect(stompConfig.reconnectDelay).toBe(5000);
  });

  it('should activate the client when connecting', () => {
    const states: string[] = [];
    service.connectionState$.subscribe((state) => states.push(state));

    service.connect();

    expect(fakeClient.activate).toHaveBeenCalled();
    expect(states).toEqual(['DISCONNECTED', 'CONNECTING']);
  });

  it('should subscribe to a room topic after the STOMP connection opens', () => {
    const receivedEvents: RealtimeEvent[] = [];

    const subscription = service.subscribeToRoom('ABCD12').subscribe((event) => {
      receivedEvents.push(event);
    });

    expect(fakeClient.activate).toHaveBeenCalled();
    expect(fakeClient.subscribe).not.toHaveBeenCalled();

    fakeClient.connected = true;
    stompConfig.onConnect?.({} as never);

    expect(fakeClient.subscribe).toHaveBeenCalled();
    expect(fakeClient.lastDestination).toBe('/topic/rooms/ABCD12');

    fakeClient.lastCallback?.({
      body: JSON.stringify({ type: 'PLAYER_JOINED', payload: { nickname: 'Alex' } }),
    } as IMessage);

    expect(receivedEvents).toEqual([
      {
        type: 'PLAYER_JOINED',
        payload: { nickname: 'Alex' },
      },
    ]);

    subscription.unsubscribe();
    expect(fakeClient.unsubscribeSpy).toHaveBeenCalled();
  });

  it('should subscribe immediately when the client is already connected', () => {
    fakeClient.connected = true;

    service.subscribeToRoom('WXYZ99').subscribe();

    expect(fakeClient.activate).not.toHaveBeenCalled();
    expect(fakeClient.lastDestination).toBe('/topic/rooms/WXYZ99');
  });

  it('should publish join-live after the STOMP connection opens', () => {
    service.joinLive('ABCD12');

    expect(fakeClient.activate).toHaveBeenCalled();
    expect(fakeClient.publish).not.toHaveBeenCalled();

    fakeClient.connected = true;
    stompConfig.onConnect?.({} as never);

    expect(fakeClient.publish).toHaveBeenCalledWith({
      destination: '/app/rooms/ABCD12/join-live',
      body: JSON.stringify({ playerId: 'player-1' }),
    });
  });

  it('should publish join-live immediately when already connected', () => {
    fakeClient.connected = true;

    service.joinLive('WXYZ99');

    expect(fakeClient.activate).not.toHaveBeenCalled();
    expect(fakeClient.publish).toHaveBeenCalledWith({
      destination: '/app/rooms/WXYZ99/join-live',
      body: JSON.stringify({ playerId: 'player-2' }),
    });
  });

  it('should publish chat messages after the STOMP connection opens', () => {
    service.sendChatMessage('ABCD12', 'Hallo');

    expect(fakeClient.activate).toHaveBeenCalled();
    expect(fakeClient.publish).not.toHaveBeenCalled();

    fakeClient.connected = true;
    stompConfig.onConnect?.({} as never);

    expect(fakeClient.publish).toHaveBeenCalledWith({
      destination: '/app/rooms/ABCD12/chat',
      body: JSON.stringify({ playerId: 'player-1', body: 'Hallo' }),
    });
  });

  it('should remove pending room subscriptions when unsubscribed before connection', () => {
    const subscription = service.subscribeToRoom('ABCD12').subscribe();

    subscription.unsubscribe();
    fakeClient.connected = true;
    stompConfig.onConnect?.({} as never);

    expect(fakeClient.subscribe).not.toHaveBeenCalled();
  });

  it('should deactivate the client when disconnecting', () => {
    service.disconnect();

    expect(fakeClient.deactivate).toHaveBeenCalled();
  });
});
