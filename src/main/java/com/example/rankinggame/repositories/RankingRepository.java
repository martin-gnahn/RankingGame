package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.RankedAnswerEntity;

import java.util.List;
import java.util.UUID;

public interface RankingRepository {
    RankedAnswerEntity save(RankedAnswerEntity rankedAnswerEntity);

    List<RankedAnswerEntity> findByRoundIdOrderByPositionAsc(UUID roundId);
}
