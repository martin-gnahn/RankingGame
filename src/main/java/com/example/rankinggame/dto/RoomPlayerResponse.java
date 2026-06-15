package com.example.rankinggame.dto;

import java.util.UUID;

public record RoomPlayerResponse(
        UUID playerId,
        String nickname,
        boolean host,
        String connectionStatus
) {
}
