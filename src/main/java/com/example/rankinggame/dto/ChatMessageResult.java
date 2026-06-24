package com.example.rankinggame.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResult(
        UUID messageId,
        String roomCode,
        UUID playerId,
        String senderNickname,
        String body,
        Instant createdAt
) {
}
