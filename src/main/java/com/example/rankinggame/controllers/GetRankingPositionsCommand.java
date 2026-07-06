package com.example.rankinggame.controllers;

import java.util.UUID;

public record GetRankingPositionsCommand(
        String roomCode, UUID roundId, UUID playerId
) implements RankingPositionCommand {
}
