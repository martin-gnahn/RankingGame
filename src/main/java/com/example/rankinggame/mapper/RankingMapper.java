package com.example.rankinggame.mapper;

import com.example.rankinggame.engine.Ranking;
import com.example.rankinggame.engine.RankingId;
import com.example.rankinggame.engine.Round;
import com.example.rankinggame.entities.AnswerEntity;
import com.example.rankinggame.entities.RankingEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RankingMapper {

    private final AnswerMapper answerMapper;

    public RankingMapper(AnswerMapper answerMapper) {
        this.answerMapper = answerMapper;
    }

    public RankingEntity toEntity(Ranking ranking, Round round) {
        AnswerEntity answerEntity = answerMapper.toEntity(round.getId(), ranking.getAnswer());
        return new RankingEntity(ranking.getId().value(), answerEntity, round.getId().value(), ranking.getOneBasedPosition());
    }

    public Ranking toDomain(RankingEntity rankingEntity) {
        return Ranking.builder()
                .id(new RankingId(rankingEntity.getId()))
                .answer(answerMapper.toSubmittedAnswer(rankingEntity.getAnswer()))
                .oneBasedPosition(rankingEntity.getPosition())
                .build();
    }

    public List<Ranking> toDomainObjects(List<RankingEntity> rankingEntities) {
        return rankingEntities.stream().map(this::toDomain).toList();
    }

//    public Ranking toDomain(RankingEntity ranking) {
//        return null;
//    }
}
