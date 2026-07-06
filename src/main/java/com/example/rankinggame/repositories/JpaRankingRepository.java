package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.AnswerEntity;
import com.example.rankinggame.entities.RankedAnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaRankingRepository extends JpaRepository<RankedAnswerEntity, UUID>, RankingRepository {
    @Query("""
                select coalesce(max(r.position), 0)
                from RankedAnswerEntity r
                where r.roundId = :roundId
            """)
    int findMaxPositionByRoundId(UUID roundId);

    List<RankedAnswerEntity> findByRoundIdOrderByPositionAsc(UUID roundId);

    Optional<RankedAnswerEntity> findByRoundIdAndAnswer(UUID roundId, AnswerEntity answer);
}
