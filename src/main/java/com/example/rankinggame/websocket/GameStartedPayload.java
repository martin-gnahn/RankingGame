package com.example.rankinggame.websocket;

import com.example.rankinggame.entities.GameType;

import java.util.UUID;

public record GameStartedPayload(
        UUID gameSessionId,
        GameType gameType
) {
}
