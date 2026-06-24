package com.example.rankinggame.events;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageSentEvent(
        String roomCode,
        UUID messageId,
        UUID playerId,
        String senderNickname,
        String body,
        Instant createdAt
) {
}
