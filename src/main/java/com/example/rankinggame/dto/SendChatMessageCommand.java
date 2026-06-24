package com.example.rankinggame.dto;

import java.util.UUID;

public record SendChatMessageCommand(
        String roomCode,
        UUID playerId,
        String body
) {
}
