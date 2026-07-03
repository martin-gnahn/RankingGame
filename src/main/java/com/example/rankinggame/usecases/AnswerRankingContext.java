package com.example.rankinggame.usecases;

import com.example.rankinggame.entities.AnswerEntity;
import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.entities.RoundEntity;

public record AnswerRankingContext(
        RoundEntity round,
        AnswerEntity answer,
        PlayerEntity captainPlayer
) {

}
