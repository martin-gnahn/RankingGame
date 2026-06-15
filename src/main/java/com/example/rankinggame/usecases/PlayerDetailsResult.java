package com.example.rankinggame.usecases;

import com.example.rankinggame.entities.PlayerConnectionStatus;

import java.util.UUID;

public record PlayerDetailsResult(
        UUID playerId,
        String nickname,
        boolean host,
        PlayerConnectionStatus connectionStatus
) {
}
