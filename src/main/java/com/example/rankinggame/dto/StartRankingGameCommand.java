package com.example.rankinggame.dto;

import java.util.UUID;

public record StartRankingGameCommand(
        String roomCode,
        UUID hostPlayerId
) implements RoomCommand {
}
