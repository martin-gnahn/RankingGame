package com.example.rankinggame.controllers;

import com.example.rankinggame.dto.RoomCommand;

import java.util.UUID;

public interface RankingPositionCommand extends RoomCommand {
    String roomCode();

    UUID roundId();

    UUID playerId();
}
