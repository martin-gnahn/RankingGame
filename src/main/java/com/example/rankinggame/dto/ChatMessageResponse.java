package com.example.rankinggame.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
        UUID messageId,
        UUID playerId,
        String senderNickname,
        String body,
        Instant createdAt
) {
}
