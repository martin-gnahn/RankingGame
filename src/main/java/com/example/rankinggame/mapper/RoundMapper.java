package com.example.rankinggame.mapper;

import com.example.rankinggame.engine.*;
import com.example.rankinggame.engine.exceptions.CaptainNotFoundException;
import com.example.rankinggame.entities.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class RoundMapper {
    private final QuestionMapper questionMapper;
    private final AnswerMapper answerMapper;
    private final RankingMapper rankingMapper;
    // Maybe i dont need dedicated PlayerMapper
    private final PlayerMapper playerMapper;

    public Round toDomain(RoundEntity roundEntity, PlayerEntity captain, List<AnswerEntity> submittedAnswers, List<RankingEntity> submittedRankings) {
        return Round.builder()
                .id(new RoundId(roundEntity.getId()))
                .submittedAnswers(answerMapper.toDomainMap(submittedAnswers))
                .answerRankings(rankingMapper.toDomainObjects(submittedRankings))
                .roundStatus(toDomainStatus(roundEntity.getState()))
                .captain(playerMapper.toParticipant(captain))
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
                        // TODO: is it clean to throw CaptainNotFoundException here?
                        .orElseThrow(CaptainNotFoundException::new)
        );
        roundEntity.setId(round.getId().value());
        roundEntity.setQuestionEntity(questionMapper.toEntity(round.getQuestion()));
        return roundEntity;
    }

    private RoundStatus toDomainStatus(RoundState state) {
        return state == null ? null : RoundStatus.valueOf(state.name());
    }

    public RoundState toEntityState(RoundStatus status) {
        return status == null ? null : RoundState.valueOf(status.name());
    }

    private Question toDomainQuestion(QuestionEntity questionEntity) {
        return questionEntity == null ? null : questionMapper.toDomain(questionEntity);
    }
}
