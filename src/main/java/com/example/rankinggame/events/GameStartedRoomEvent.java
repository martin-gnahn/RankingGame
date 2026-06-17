package com.example.rankinggame.events;

import com.example.rankinggame.entities.GameType;

import java.util.UUID;

public record GameStartedRoomEvent(
        String roomCode,
        UUID gameSessionId,
        GameType gameType
) {
}
