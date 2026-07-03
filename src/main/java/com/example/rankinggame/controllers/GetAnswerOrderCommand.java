package com.example.rankinggame.controllers;

import com.example.rankinggame.dto.RoomCommand;

import java.util.UUID;

public record GetAnswerOrderCommand(
        String roomCode, UUID roundId, UUID playerId
) implements RoomCommand {
}
