package com.example.rankinggame.websocket;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PlayerDisconnectListenerTest {
    @Test
    void delegatesSessionDisconnectToPresenceService() {
        PlayerPresenceService playerPresenceService = mock(PlayerPresenceService.class);
        PlayerDisconnectListener listener = new PlayerDisconnectListener(playerPresenceService);

        listener.handleDisconnect(disconnectEvent("session-1"));

        verify(playerPresenceService).markSessionDisconnected("session-1");
    }

    private SessionDisconnectEvent disconnectEvent(String sessionId) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.DISCONNECT);
        accessor.setSessionId(sessionId);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        return new SessionDisconnectEvent(this, message, sessionId, CloseStatus.NORMAL);
    }
}
