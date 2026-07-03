package com.example.rankinggame.controllers;

import java.util.UUID;

public record GetAnswerOrderCommand(
        String roomCode, UUID roundId, UUID playerId
) {
}
