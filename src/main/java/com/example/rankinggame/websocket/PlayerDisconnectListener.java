package com.example.rankinggame.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@RequiredArgsConstructor
@Component
public class PlayerDisconnectListener {
    private final PlayerPresenceService playerPresenceService;

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        playerPresenceService.markSessionDisconnected(event.getSessionId());
    }
}
