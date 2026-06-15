package com.example.rankinggame.dto;

import java.util.List;
import java.util.UUID;

public record RoomResponse(
        UUID roomId,
        String roomCode,
        String status,
        List<RoomPlayerResponse> players
) {
}
