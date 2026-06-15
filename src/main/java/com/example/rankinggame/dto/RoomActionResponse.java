package com.example.rankinggame.dto;

import java.util.UUID;

public record RoomActionResponse(String roomCode, UUID playerId) {
}
