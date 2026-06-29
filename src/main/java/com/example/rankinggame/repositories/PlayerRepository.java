package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.PlayerEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerRepository {
    PlayerEntity save(PlayerEntity player);

    void flush();

    Optional<PlayerEntity> findById(UUID id);

    List<PlayerEntity> findByRoomId(UUID roomId);

    List<PlayerEntity> findByGameSessionId(UUID gameSessionId);

    // List<PlayerEntity> findByRoomCode(String roomCode);
}
