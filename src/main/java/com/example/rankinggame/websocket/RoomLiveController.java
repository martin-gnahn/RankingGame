package com.example.rankinggame.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@RequiredArgsConstructor
@Controller
public class RoomLiveController {
    private final LivePlayerSessionRegistry sessionRegistry;

    @MessageMapping("/rooms/{roomCode}/join-live")
    public void joinLive(
            @DestinationVariable String roomCode,
            @Payload JoinLiveRequest request,
            @Header("simpSessionId") String sessionId
    ) {
        if (request == null) {
            return;
        }

        sessionRegistry.register(sessionId, roomCode, request.playerId());
    }
}
