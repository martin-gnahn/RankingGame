package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.GameSessionPlayerEntity;
import com.example.rankinggame.entities.GameSessionPlayerId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaGameSessionPlayerRepository
        extends JpaRepository<GameSessionPlayerEntity, GameSessionPlayerId>, GameSessionPlayerRepository {
}
