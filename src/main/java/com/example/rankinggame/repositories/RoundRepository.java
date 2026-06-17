package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.Round;

import java.util.List;
import java.util.UUID;

public interface RoundRepository {
    Round save(Round round);

    List<Round> findByGameSessionId(UUID gameSessionId);
}
