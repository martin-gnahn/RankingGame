package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaGameSessionRepository extends JpaRepository<GameSession, UUID>, GameSessionRepository {
}
