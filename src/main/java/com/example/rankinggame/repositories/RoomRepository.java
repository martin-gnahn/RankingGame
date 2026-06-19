package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.RoomEntity;

import java.util.Optional;
import java.util.UUID;

public interface RoomRepository {
    RoomEntity save(RoomEntity room);

    void flush();

    Optional<RoomEntity> findById(UUID id);

    Optional<RoomEntity> findByCode(String code);

    boolean existsByCode(String code);
}
