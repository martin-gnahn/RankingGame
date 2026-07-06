package com.example.rankinggame.dto;

import com.example.rankinggame.controllers.RankingPositionCommand;

import java.util.UUID;

public record AddRankingPositionCommand(
        String roomCode,
        UUID roundId,
        UUID playerId,
        UUID answerId
) implements RoomCommand, RankingPositionCommand {
}
