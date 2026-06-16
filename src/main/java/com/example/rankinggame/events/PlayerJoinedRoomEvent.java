package com.example.rankinggame.events;

import java.util.UUID;

public record PlayerJoinedRoomEvent(
        String roomCode,
        UUID playerId,
        String nickname,
        boolean host
) {
}
