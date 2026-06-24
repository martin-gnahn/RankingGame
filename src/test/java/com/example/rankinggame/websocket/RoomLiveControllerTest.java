package com.example.rankinggame.websocket;

import com.example.rankinggame.dto.SendChatMessageCommand;
import com.example.rankinggame.usecases.ChatMessageService;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RoomLiveControllerTest {
    @Test
    void registersPlayerSessionForRoom() {
        LivePlayerSessionRegistry registry = new LivePlayerSessionRegistry();
        ChatMessageService chatMessageService = mock(ChatMessageService.class);
        RoomLiveController controller = new RoomLiveController(registry, chatMessageService);
        UUID playerId = UUID.randomUUID();

        controller.joinLive("abcd12", new JoinLiveRequest(playerId), "session-1");

        Optional<LivePlayerSession> session = registry.remove("session-1");
        assertThat(session).contains(new LivePlayerSession("ABCD12", playerId));
    }

    @Test
    void sendsChatMessageForRoom() {
        LivePlayerSessionRegistry registry = new LivePlayerSessionRegistry();
        ChatMessageService chatMessageService = mock(ChatMessageService.class);
        RoomLiveController controller = new RoomLiveController(registry, chatMessageService);
        UUID playerId = UUID.randomUUID();

        controller.sendChatMessage("ABCD12", new SendChatMessagePayload(playerId, "Hallo"));

        verify(chatMessageService).sendMessage(new SendChatMessageCommand("ABCD12", playerId, "Hallo"));
    }
}
