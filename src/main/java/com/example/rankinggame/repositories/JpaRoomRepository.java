package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaRoomRepository extends JpaRepository<RoomEntity, UUID>, RoomRepository {
}
