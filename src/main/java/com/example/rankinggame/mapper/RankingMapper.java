package com.example.rankinggame.mapper;

import com.example.rankinggame.engine.Ranking;
import com.example.rankinggame.engine.Round;
import com.example.rankinggame.entities.AnswerEntity;
import com.example.rankinggame.entities.RankingEntity;
import org.springframework.stereotype.Service;

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

//    public Ranking toDomain(RankingEntity ranking) {
//        return null;
//    }
}
