package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.AnswerEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnswerRepository {
    AnswerEntity save(AnswerEntity answer);

    AnswerEntity saveAndFlush(AnswerEntity answer);

    Optional<AnswerEntity> findByRoundIdAndPlayerId(UUID roundId, UUID playerId);

    int countByRoundId(UUID roundId);

    List<AnswerEntity> findByRoundIdOrderBySubmittedAtAsc(UUID roundId);

    Optional<AnswerEntity> findById(UUID answerId);
}
