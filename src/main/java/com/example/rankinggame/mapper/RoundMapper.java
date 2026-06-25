package com.example.rankinggame.mapper;

import com.example.rankinggame.engine.GameParticipant;
import com.example.rankinggame.engine.PlayerId;
import com.example.rankinggame.engine.Question;
import com.example.rankinggame.engine.Round;
import com.example.rankinggame.engine.RoundStatus;
import com.example.rankinggame.entities.QuestionEntity;
import com.example.rankinggame.entities.RoundEntity;
import com.example.rankinggame.entities.RoundState;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

@Service
public class RoundMapper {
    private final QuestionMapper questionMapper;

    public RoundMapper(QuestionMapper questionMapper) {
        this.questionMapper = questionMapper;
    }

    public Round toDomain(RoundEntity roundEntity) {
        return toDomain(roundEntity, toCaptainProjection(roundEntity.getCaptainPlayerId()));
    }

    public Round toDomain(RoundEntity roundEntity, GameParticipant captain) {
        return Round.builder()
                .submittedAnswers(new HashMap<>())
                .roundStatus(toDomainStatus(roundEntity.getState()))
                .captain(captain)
                .question(toDomainQuestion(roundEntity.getQuestionEntity()))
                .build();
    }

    public RoundEntity toEntity(Round round) {
        RoundEntity roundEntity = new RoundEntity();
        roundEntity.setState(toEntityState(round.getRoundStatus()));
        roundEntity.setCaptainPlayerId(
                Optional.ofNullable(round.getCaptain())
                        .map(GameParticipant::playerId)
                        .map(PlayerId::value)
                        .orElse(null)
        );
        roundEntity.setQuestionEntity(questionMapper.toEntity(round.getQuestion()));
        return roundEntity;
    }

    private RoundStatus toDomainStatus(RoundState state) {
        return state == null ? null : RoundStatus.valueOf(state.name());
    }

    private RoundState toEntityState(RoundStatus status) {
        return status == null ? null : RoundState.valueOf(status.name());
    }

    private GameParticipant toCaptainProjection(UUID captainPlayerId) {
        return captainPlayerId == null ? null : new GameParticipant(new PlayerId(captainPlayerId), null, false);
    }

    private Question toDomainQuestion(QuestionEntity questionEntity) {
        return questionEntity == null ? null : questionMapper.toDomain(questionEntity);
    }
}
