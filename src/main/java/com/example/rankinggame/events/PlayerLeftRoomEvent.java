package com.example.rankinggame.events;

import java.util.UUID;

public record PlayerLeftRoomEvent(
        String roomCode,
        UUID playerId
) {
}
