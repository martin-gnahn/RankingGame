package com.example.rankinggame.usecases;

import com.example.rankinggame.entities.AnswerEntity;
import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.entities.RoundEntity;

import java.util.Optional;

public record AnswerRankingContext(
        RoundEntity round,
        Optional<AnswerEntity> answer,
        PlayerEntity captainPlayer
) {

}
