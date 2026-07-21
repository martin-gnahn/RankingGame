package com.example.rankinggame.websocket;

import com.example.rankinggame.dto.SendChatMessageCommand;
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

    @MessageMapping("/rooms/{roomCode}/join-live")
    public void joinLive(
            @DestinationVariable String roomCode,
            @Payload JoinLiveRequest request,
            @Header("simpSessionId") String sessionId
    ) {
        if (request == null) {
            return;
        }

        playerPresenceService.markConnected(sessionId, roomCode, request.playerId());
    }

    @MessageMapping("/rooms/{roomCode}/chat")
    public void sendChatMessage(
            @DestinationVariable String roomCode,
            @Payload SendChatMessagePayload request
    ) {
        if (request == null) {
            return;
        }

        SendChatMessageCommand command = new SendChatMessageCommand(
                roomCode,
                request.playerId(),
                request.body()
        );
        chatMessageService.sendMessage(command);
    }
}
