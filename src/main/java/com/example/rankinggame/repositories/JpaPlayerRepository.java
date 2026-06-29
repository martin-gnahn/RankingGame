package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface JpaPlayerRepository extends JpaRepository<PlayerEntity, UUID>, PlayerRepository {
    @Override
    @Query("""
            select p
            from PlayerEntity p
            join GameSessionPlayerEntity gsp on gsp.playerId = p.id
            where gsp.gameSessionId = :gameSessionId
            order by p.joinedAt
            """)
    List<PlayerEntity> findByGameSessionId(UUID gameSessionId);

    // List<PlayerEntity> findByRoomCode(String roomCode);
}
