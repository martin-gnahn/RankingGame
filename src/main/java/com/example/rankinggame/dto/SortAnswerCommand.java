package com.example.rankinggame.dto;

import java.util.UUID;

public record SortAnswerCommand(
        String roomCode,
        UUID roundId,
        UUID playerId,
        UUID answerId
) implements RoomCommand {
}
