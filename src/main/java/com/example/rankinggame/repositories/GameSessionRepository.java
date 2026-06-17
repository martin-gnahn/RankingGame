package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.GameSession;

import java.util.Optional;
import java.util.UUID;

public interface GameSessionRepository {
    GameSession save(GameSession gameSession);

    Optional<GameSession> findById(UUID id);

    Optional<GameSession> findByRoomId(UUID roomId);
}
