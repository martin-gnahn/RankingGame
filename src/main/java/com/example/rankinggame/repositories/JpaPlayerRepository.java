package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaPlayerRepository extends JpaRepository<PlayerEntity, UUID>, PlayerRepository {
    // List<PlayerEntity> findByRoomCode(String roomCode);
}
