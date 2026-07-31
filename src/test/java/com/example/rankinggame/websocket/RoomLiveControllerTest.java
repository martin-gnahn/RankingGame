package com.example.rankinggame.websocket;

import com.example.rankinggame.controllers.AuthenticatedPlayer;
import com.example.rankinggame.controllers.PlayerSessionService;
import com.example.rankinggame.dto.SendChatMessageCommand;
import com.example.rankinggame.usecases.ChatMessageService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RoomLiveControllerTest {

    private static final String PLAYER_TOKEN = "player-token";
    private static final String ROOM_CODE_UC = "ABCD12";
    private static final String ROOM_CODE_LC = "abcd12";
    private static final String CHAT_MESSAGE = "Hallo";

    @Test
    void registersPlayerSessionForRoom() {
        ChatMessageService chatMessageService = mock(ChatMessageService.class);
        PlayerPresenceService playerPresenceService = mock(PlayerPresenceService.class);
        PlayerSessionService playerSessionService = mock(PlayerSessionService.class);
        RoomLiveController controller = new RoomLiveController(chatMessageService, playerPresenceService, playerSessionService);
        UUID playerId = UUID.randomUUID();
        Mockito.when(playerSessionService.authenticatePlayer(ROOM_CODE_LC, PLAYER_TOKEN)).thenReturn(
                new AuthenticatedPlayer(playerId)
        );

        controller.joinLive(ROOM_CODE_LC, "session-1", PLAYER_TOKEN);

        verify(playerPresenceService).markConnected("session-1", ROOM_CODE_LC, playerId);
    }

    @Test
    void sendsChatMessageForRoom() {
        ChatMessageService chatMessageService = mock(ChatMessageService.class);
        PlayerPresenceService playerPresenceService = mock(PlayerPresenceService.class);
        PlayerSessionService playerSessionService = mock(PlayerSessionService.class);
        RoomLiveController controller = new RoomLiveController(chatMessageService, playerPresenceService, playerSessionService);
        UUID playerId = UUID.randomUUID();
        Mockito.when(playerSessionService.authenticatePlayer(ROOM_CODE_UC, PLAYER_TOKEN)).thenReturn(
                new AuthenticatedPlayer(playerId)
        );

        controller.sendChatMessage(ROOM_CODE_UC, new SendChatMessagePayload(CHAT_MESSAGE), PLAYER_TOKEN);

        verify(chatMessageService).sendMessage(new SendChatMessageCommand(ROOM_CODE_UC, playerId, CHAT_MESSAGE));
    }
}
