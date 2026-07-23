package com.example.rankinggame.websocket;

import com.example.rankinggame.controllers.AuthenticatedPlayer;
import com.example.rankinggame.controllers.PlayerSessionService;
import com.example.rankinggame.dto.SendChatMessageCommand;
import com.example.rankinggame.engine.GameConstants;
import com.example.rankinggame.usecases.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@RequiredArgsConstructor
@Controller
public class RoomLiveController {
    private final ChatMessageService chatMessageService;
    private final PlayerPresenceService playerPresenceService;
    private final PlayerSessionService playerSessionService;

    @MessageMapping("/rooms/{roomCode}/join-live")
    public void joinLive(
            @DestinationVariable String roomCode,
            @Header("simpSessionId") String sessionId,
            @Header(value = GameConstants.PLAYER_SESSION_TOKEN, required = false) String token
    ) {
        AuthenticatedPlayer player =
                playerSessionService.authenticatePlayer(roomCode, token);
        playerPresenceService.markConnected(sessionId, roomCode, player.playerId());
    }

    @MessageMapping("/rooms/{roomCode}/chat")
    public void sendChatMessage(
            @DestinationVariable String roomCode,
            @Payload SendChatMessagePayload request,
            @Header(value = GameConstants.PLAYER_SESSION_TOKEN, required = false) String token
    ) {
        if (request == null) {
            return;
        }

        AuthenticatedPlayer player =
                playerSessionService.authenticatePlayer(roomCode, token);
        SendChatMessageCommand command = new SendChatMessageCommand(
                roomCode,
                player.playerId(),
                request.body()
        );
        chatMessageService.sendMessage(command);
    }
}
