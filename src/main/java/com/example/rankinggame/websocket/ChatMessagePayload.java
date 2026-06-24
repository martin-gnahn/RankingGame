package com.example.rankinggame.websocket;

import java.time.Instant;
import java.util.UUID;

public record ChatMessagePayload(
        UUID messageId,
        UUID playerId,
        String senderNickname,
        String body,
        Instant createdAt
) {
}
