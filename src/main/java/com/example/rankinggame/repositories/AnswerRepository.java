package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.Answer;

import java.util.UUID;

public interface AnswerRepository {
    Answer save(Answer answer);

    boolean existsByRoundIdAndPlayerId(UUID roundId, UUID playerId);
}
