package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerRepository {
    Player save(Player player);

    Optional<Player> findById(UUID id);

    List<Player> findByRoomId(UUID roomId);
}
