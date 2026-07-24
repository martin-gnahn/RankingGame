package com.example.rankinggame.dto;

import java.util.UUID;

public record RoomActionResponse(
        String roomCode,
        UUID roomId,
        UUID playerId,
        String nickname,
        String playerToken,
        boolean host
) {
}
