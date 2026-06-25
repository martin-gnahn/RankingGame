package com.example.rankinggame.dto;

import java.util.UUID;

public record GetSubmittedAnswersCommand(
        String roomCode,
        UUID roundId,
        UUID requesterPlayerId
) {
}
