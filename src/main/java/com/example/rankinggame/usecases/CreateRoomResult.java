package com.example.rankinggame.usecases;

import java.util.UUID;

public record CreateRoomResult(String roomCode, UUID roomId, UUID playerId, String playerName) {
}
