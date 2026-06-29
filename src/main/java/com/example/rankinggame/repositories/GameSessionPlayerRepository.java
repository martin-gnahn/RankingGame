package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.GameSessionPlayerEntity;

import java.util.List;
import java.util.UUID;

public interface GameSessionPlayerRepository {
    <S extends GameSessionPlayerEntity> List<S> saveAll(Iterable<S> gameSessionPlayers);

    List<GameSessionPlayerEntity> findByGameSessionId(UUID gameSessionId);
}
