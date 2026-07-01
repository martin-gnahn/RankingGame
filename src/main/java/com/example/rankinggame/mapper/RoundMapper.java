package com.example.rankinggame.mapper;

import com.example.rankinggame.engine.*;
import com.example.rankinggame.entities.AnswerEntity;
import com.example.rankinggame.entities.QuestionEntity;
import com.example.rankinggame.entities.RoundEntity;
import com.example.rankinggame.entities.RoundState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class RoundMapper {
    private final QuestionMapper questionMapper;
    private final AnswerMapper answerMapper;

    public Round toDomain(RoundEntity roundEntity, List<AnswerEntity> otherSubmittedAnswers) {
        return toDomain(roundEntity, toCaptainProjection(roundEntity.getCaptainPlayerId()), otherSubmittedAnswers);
    }

    public Round toDomain(RoundEntity roundEntity, GameParticipant captain, List<AnswerEntity> otherSubmittedAnswers) {
        return Round.builder()
                .id(new RoundId(roundEntity.getId()))
                .submittedAnswers(answerMapper.toDomainMap(otherSubmittedAnswers))
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
        roundEntity.setId(round.getId().value());
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
