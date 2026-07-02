package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.RoundEntity;
import com.example.rankinggame.entities.RoundState;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface JpaRoundRepository extends JpaRepository<RoundEntity, UUID>, RoundRepository {
    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select round from RoundEntity round where round.id = :id")
    Optional<RoundEntity> findByIdForUpdate(UUID id);

    @Override
    @Modifying
    @Query("""
            update RoundEntity round
            set round.state = :newState
            where round.id = :id
              and round.state = :expectedState
            """)
    int updateStateIfCurrent(UUID id, RoundState expectedState, RoundState newState);
}
