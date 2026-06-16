package com.example.rankinggame.websocket;

import java.util.UUID;

public record PlayerJoinedPayload(
        UUID playerId,
        String nickname,
        boolean host
) {
}
