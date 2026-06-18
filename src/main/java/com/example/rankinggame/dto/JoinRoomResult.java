package com.example.rankinggame.dto;

import java.util.UUID;

public record JoinRoomResult(String roomCode, UUID roomId, UUID playerId, String playerName) {
}
