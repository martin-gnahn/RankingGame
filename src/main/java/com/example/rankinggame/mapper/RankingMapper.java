package com.example.rankinggame.mapper;

import com.example.rankinggame.engine.RankedAnswer;
import com.example.rankinggame.engine.RankingId;
import com.example.rankinggame.engine.Round;
import com.example.rankinggame.entities.AnswerEntity;
import com.example.rankinggame.entities.RankedAnswerEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RankingMapper {

    private final AnswerMapper answerMapper;

    public RankingMapper(AnswerMapper answerMapper) {
        this.answerMapper = answerMapper;
    }

    public RankedAnswerEntity toEntity(RankedAnswer rankedAnswer, Round round) {
        AnswerEntity answerEntity = answerMapper.toEntity(round.getId(), rankedAnswer.getAnswer());
        return new RankedAnswerEntity(rankedAnswer.getId().value(), answerEntity, round.getId().value(), rankedAnswer.getOneBasedPosition());
    }

    public RankedAnswer toDomain(RankedAnswerEntity rankedAnswerEntity) {
        return RankedAnswer.builder()
                .id(new RankingId(rankedAnswerEntity.getId()))
                .answer(answerMapper.toSubmittedAnswer(rankedAnswerEntity.getAnswer()))
                .oneBasedPosition(rankedAnswerEntity.getPosition())
                .build();
    }

    public List<RankedAnswer> toDomainObjects(List<RankedAnswerEntity> rankingEntities) {
        return rankingEntities.stream().map(this::toDomain).collect(Collectors.toList());
    }

//    public Ranking toDomain(RankingEntity ranking) {
//        return null;
//    }
}
