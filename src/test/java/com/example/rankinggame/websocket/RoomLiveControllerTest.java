package com.example.rankinggame.websocket;

import com.example.rankinggame.controllers.PlayerSessionService;
import com.example.rankinggame.dto.SendChatMessageCommand;
import com.example.rankinggame.usecases.ChatMessageService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RoomLiveControllerTest {
    @Test
    void registersPlayerSessionForRoom() {
        ChatMessageService chatMessageService = mock(ChatMessageService.class);
        PlayerPresenceService playerPresenceService = mock(PlayerPresenceService.class);
        PlayerSessionService playerSessionService = mock(PlayerSessionService.class);
        RoomLiveController controller = new RoomLiveController(chatMessageService, playerPresenceService, playerSessionService);
        UUID playerId = UUID.randomUUID();

        controller.joinLive("abcd12", "session-1", "player-token");

        verify(playerPresenceService).markConnected("session-1", "abcd12", playerId);
    }

    @Test
    void sendsChatMessageForRoom() {
        ChatMessageService chatMessageService = mock(ChatMessageService.class);
        PlayerPresenceService playerPresenceService = mock(PlayerPresenceService.class);
        PlayerSessionService playerSessionService = mock(PlayerSessionService.class);
        RoomLiveController controller = new RoomLiveController(chatMessageService, playerPresenceService, playerSessionService);
        UUID playerId = UUID.randomUUID();

        controller.sendChatMessage("ABCD12", new SendChatMessagePayload("Hallo"), "player-token");

        verify(chatMessageService).sendMessage(new SendChatMessageCommand("ABCD12", playerId, "Hallo"));
    }
}
