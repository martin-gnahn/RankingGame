package com.example.rankinggame.usecases;

import com.example.rankinggame.entities.GameSession;
import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.entities.RoomEntity;
import com.example.rankinggame.entities.RoundEntity;

public record AnswerSubmissionContext(
        RoomEntity room,
        PlayerEntity player,
        RoundEntity round,
        GameSession gameSession
) {

}
