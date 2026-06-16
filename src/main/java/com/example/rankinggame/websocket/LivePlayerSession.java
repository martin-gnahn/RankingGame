package com.example.rankinggame.websocket;

import java.util.UUID;

public record LivePlayerSession(
        String roomCode,
        UUID playerId
) {
}
