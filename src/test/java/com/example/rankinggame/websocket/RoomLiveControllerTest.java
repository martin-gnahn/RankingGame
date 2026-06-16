package com.example.rankinggame.websocket;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RoomLiveControllerTest {
    @Test
    void registersPlayerSessionForRoom() {
        LivePlayerSessionRegistry registry = new LivePlayerSessionRegistry();
        RoomLiveController controller = new RoomLiveController(registry);
        UUID playerId = UUID.randomUUID();

        controller.joinLive("abcd12", new JoinLiveRequest(playerId), "session-1");

        Optional<LivePlayerSession> session = registry.remove("session-1");
        assertThat(session).contains(new LivePlayerSession("ABCD12", playerId));
    }
}
