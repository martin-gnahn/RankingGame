package com.example.rankinggame.events;

import java.util.UUID;

public record PlayerRejoinedRoomEvent(
        String roomCode,
        UUID playerId
) {
}
