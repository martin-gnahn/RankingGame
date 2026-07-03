package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.AnswerEntity;
import com.example.rankinggame.entities.RankingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaRankingRepository extends JpaRepository<RankingEntity, UUID>, RankingRepository {
    @Query("""
                select coalesce(max(r.position), 0)
                from RankingEntity r
                where r.roundId = :roundId
            """)
    int findMaxPositionByRoundId(UUID roundId);

    List<RankingEntity> findByRoundIdOrderByPositionAsc(UUID roundId);

    Optional<RankingEntity> findByRoundIdAndAnswer(UUID roundId, AnswerEntity answer);
}
