package com.example.rankinggame.websocket;

import com.example.rankinggame.controllers.AuthenticatedPlayer;
import com.example.rankinggame.controllers.PlayerSessionService;
import com.example.rankinggame.dto.SendChatMessageCommand;
import com.example.rankinggame.usecases.ChatMessageService;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomLiveControllerTest {
    @Test
    void registersPlayerSessionForRoom() {
        LivePlayerSessionRegistry registry = new LivePlayerSessionRegistry();
        ChatMessageService chatMessageService = mock(ChatMessageService.class);
        PlayerSessionService playerSessionService = mock(PlayerSessionService.class);
        UUID playerId = UUID.randomUUID();
        when(playerSessionService.authenticatePlayer("abcd12", "token"))
                .thenReturn(new AuthenticatedPlayer(playerId));
        RoomLiveController controller = new RoomLiveController(registry, chatMessageService, playerSessionService);

        controller.joinLive("abcd12", "session-1", "token");

        Optional<LivePlayerSession> session = registry.remove("session-1");
        assertThat(session).contains(new LivePlayerSession("ABCD12", playerId));
    }

    @Test
    void sendsChatMessageForRoom() {
        LivePlayerSessionRegistry registry = new LivePlayerSessionRegistry();
        ChatMessageService chatMessageService = mock(ChatMessageService.class);
        PlayerSessionService playerSessionService = mock(PlayerSessionService.class);
        UUID playerId = UUID.randomUUID();
        when(playerSessionService.authenticatePlayer("ABCD12", "token"))
                .thenReturn(new AuthenticatedPlayer(playerId));
        RoomLiveController controller = new RoomLiveController(registry, chatMessageService, playerSessionService);

        controller.sendChatMessage("ABCD12", new SendChatMessagePayload("Hallo"), "token");

        verify(chatMessageService).sendMessage(new SendChatMessageCommand("ABCD12", playerId, "Hallo"));
    }
}
