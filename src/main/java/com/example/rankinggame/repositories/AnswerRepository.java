package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.AnswerEntity;

import java.util.List;
import java.util.UUID;

public interface AnswerRepository {
    AnswerEntity save(AnswerEntity answer);

    boolean existsByRoundIdAndPlayerId(UUID roundId, UUID playerId);

    int countByRoundId(UUID roundId);

    List<AnswerEntity> findByRoundIdOrderBySubmittedAtAsc(UUID roundId);
}
