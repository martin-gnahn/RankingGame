package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.Round;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoundRepository {
    Round save(Round round);

    Optional<Round> findById(UUID id);

    List<Round> findByGameSessionId(UUID gameSessionId);
}
