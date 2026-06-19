package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.RoundEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoundRepository {
    RoundEntity save(RoundEntity round);

    Optional<RoundEntity> findById(UUID id);

    List<RoundEntity> findByGameSessionId(UUID gameSessionId);
}
