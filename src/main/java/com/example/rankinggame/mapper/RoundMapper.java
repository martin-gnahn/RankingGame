package com.example.rankinggame.mapper;

import com.example.rankinggame.engine.PlayerId;
import com.example.rankinggame.engine.Round;
import com.example.rankinggame.entities.RoundEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class RoundMapper {
    private final QuestionMapper questionMapper;

    public RoundMapper(QuestionMapper questionMapper) {
        this.questionMapper = questionMapper;
    }

    public Round toDomain(RoundEntity roundEntity) {
        return Round.builder()
                .submittedAnswers(new HashMap<>())
                .roundState(roundEntity.getState())
                .captainPlayerId(new PlayerId(roundEntity.getCaptainPlayerId()))
                .question(questionMapper.toDomain(roundEntity.getQuestionEntity()))
                .build();
    }

    public RoundEntity toEntity(Round round) {
        RoundEntity roundEntity = new RoundEntity();
        roundEntity.setState(round.getRoundState());
        roundEntity.setCaptainPlayerId(round.getCaptainPlayerId() == null ? null : round.getCaptainPlayerId().value());
        roundEntity.setQuestionEntity(questionMapper.toEntity(round.getQuestion()));
        return roundEntity;
    }
}
