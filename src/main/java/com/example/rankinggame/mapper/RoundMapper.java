package com.example.rankinggame.mapper;

import com.example.rankinggame.engine.GameParticipant;
import com.example.rankinggame.engine.PlayerId;
import com.example.rankinggame.engine.Round;
import com.example.rankinggame.entities.GameSession;
import com.example.rankinggame.entities.RoundEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Optional;

@Service
public class RoundMapper {
    private final QuestionMapper questionMapper;

    public RoundMapper(QuestionMapper questionMapper) {
        this.questionMapper = questionMapper;
    }

    public Round toDomain(RoundEntity roundEntity, GameParticipant captain) {
        return Round.builder()
                .submittedAnswers(new HashMap<>())
                .roundState(roundEntity.getState())
                .captain(captain)
                .question(questionMapper.toDomain(roundEntity.getQuestionEntity()))
                .build();
    }

    public RoundEntity toEntity(Round round) {
        RoundEntity roundEntity = new RoundEntity();
        roundEntity.setState(round.getRoundState());
        roundEntity.setCaptainPlayerId(
                Optional.ofNullable(round.getCaptain())
                        .map(GameParticipant::playerId)
                        .map(PlayerId::value)
                        .orElse(null)
        );
        roundEntity.setQuestionEntity(questionMapper.toEntity(round.getQuestion()));
        return roundEntity;
    }
}
