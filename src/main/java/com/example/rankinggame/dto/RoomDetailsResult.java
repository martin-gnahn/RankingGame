package com.example.rankinggame.dto;

import com.example.rankinggame.entities.RoomStatus;

import java.util.List;
import java.util.UUID;

public record RoomDetailsResult(
        UUID roomId,
        String roomCode,
        RoomStatus status,
        List<PlayerDetailsResult> players
) {
}
