package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.AnswerEntity;
import com.example.rankinggame.entities.RankedAnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaRankingRepository extends JpaRepository<RankedAnswerEntity, UUID>, RankingRepository {
    List<RankedAnswerEntity> findByRoundIdOrderByOneBasedPositionAsc(UUID roundId);
}
