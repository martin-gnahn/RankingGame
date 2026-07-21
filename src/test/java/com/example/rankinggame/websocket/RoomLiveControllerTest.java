package com.example.rankinggame.websocket;

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
        RoomLiveController controller = new RoomLiveController(chatMessageService, playerPresenceService);
        UUID playerId = UUID.randomUUID();

        controller.joinLive("abcd12", new JoinLiveRequest(playerId), "session-1");

        verify(playerPresenceService).markConnected("session-1", "abcd12", playerId);
    }

    @Test
    void sendsChatMessageForRoom() {
        ChatMessageService chatMessageService = mock(ChatMessageService.class);
        PlayerPresenceService playerPresenceService = mock(PlayerPresenceService.class);
        RoomLiveController controller = new RoomLiveController(chatMessageService, playerPresenceService);
        UUID playerId = UUID.randomUUID();

        controller.sendChatMessage("ABCD12", new SendChatMessagePayload(playerId, "Hallo"));

        verify(chatMessageService).sendMessage(new SendChatMessageCommand("ABCD12", playerId, "Hallo"));
    }
}
