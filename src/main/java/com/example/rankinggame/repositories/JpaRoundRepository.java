package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.Round;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaRoundRepository extends JpaRepository<Round, UUID>, RoundRepository {
}
