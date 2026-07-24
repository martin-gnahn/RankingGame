import {TestBed} from '@angular/core/testing';
import {Router} from '@angular/router';
import {type IMessage, type StompConfig, type StompSubscription} from '@stomp/stompjs';

import {PlayerSessionStore} from '../../shared/player-session-store';
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
  let playerSessionStore: PlayerSessionStore;
  let stompConfig: StompConfig;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    sessionStorage.clear();
    fakeClient = new FakeStompClient();
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    router.navigate.and.returnValue(Promise.resolve(true));

    TestBed.configureTestingModule({
      providers: [
        WebSocketService,
        {provide: Router, useValue: router},
        {
          provide: STOMP_CLIENT_FACTORY,
          useValue: (config: StompConfig) => {
            stompConfig = config;
            return fakeClient;
          },
        },
      ],
    });

    playerSessionStore = TestBed.inject(PlayerSessionStore);
    service = TestBed.inject(WebSocketService);
    playerSessionStore.storePlayerData({
      playerId: 'player-1',
      role: 'player',
      playerSessionToken: 'token-1',
    });
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
      headers: {
        'X-Player-Session-Token': 'token-1',
      },
    });
  });

  it('should publish join-live immediately when already connected', () => {
    fakeClient.connected = true;
    playerSessionStore.storePlayerData({
      playerId: 'player-2',
      role: 'player',
      playerSessionToken: 'token-2',
    });

    service.joinLive('WXYZ99');

    expect(fakeClient.activate).not.toHaveBeenCalled();
    expect(fakeClient.publish).toHaveBeenCalledWith({
      destination: '/app/rooms/WXYZ99/join-live',
      headers: {
        'X-Player-Session-Token': 'token-2',
      },
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
      body: JSON.stringify({body: 'Hallo'}),
      headers: {
        'X-Player-Session-Token': 'token-1',
      },
    });
  });

  it('should navigate to the error page instead of publishing without a token', () => {
    playerSessionStore.clearPlayerData();

    service.joinLive('ABCD12');
    service.sendChatMessage('ABCD12', 'Hallo');

    expect(fakeClient.activate).not.toHaveBeenCalled();
    expect(fakeClient.publish).not.toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledTimes(2);
    expect(router.navigate).toHaveBeenCalledWith(['/error']);
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
