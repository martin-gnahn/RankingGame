package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.RoundEntity;
import com.example.rankinggame.entities.RoundState;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoundRepository {
    RoundEntity save(RoundEntity round);

    Optional<RoundEntity> findById(UUID id);

    Optional<RoundEntity> findByIdForUpdate(UUID id);

    int updateStateIfCurrent(UUID id, RoundState expectedState, RoundState newState);

    List<RoundEntity> findByGameSessionId(UUID gameSessionId);
}
