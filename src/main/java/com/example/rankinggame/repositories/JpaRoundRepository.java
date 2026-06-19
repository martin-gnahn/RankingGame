package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.RoundEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaRoundRepository extends JpaRepository<RoundEntity, UUID>, RoundRepository {
}
