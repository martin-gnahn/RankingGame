package com.example.rankinggame.websocket;

import java.util.UUID;

public record SendChatMessagePayload(
        UUID playerId,
        String body
) {
}
