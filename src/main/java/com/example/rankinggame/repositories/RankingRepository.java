package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.RankingEntity;

import java.util.List;
import java.util.UUID;

public interface RankingRepository {
    RankingEntity save(RankingEntity rankingEntity);

    List<RankingEntity> findByRoundIdOrderByPositionAsc(UUID roundId);
}
