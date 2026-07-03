package com.example.rankinggame.dto;

import java.util.UUID;

public record SortAnswersCommand(
        String roomCode,
        UUID roundId,
        UUID hostPlayerId,
        UUID answerId
) implements RoomCommand {
}
