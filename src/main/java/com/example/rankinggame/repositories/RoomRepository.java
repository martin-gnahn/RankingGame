package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.Room;

import java.util.Optional;
import java.util.UUID;

public interface RoomRepository {
    Room save(Room room);

    Optional<Room> findById(UUID id);

    Optional<Room> findByCode(String code);

    boolean existsByCode(String code);
}
