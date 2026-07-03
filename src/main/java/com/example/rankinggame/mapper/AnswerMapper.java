package com.example.rankinggame.mapper;

import com.example.rankinggame.engine.*;
import com.example.rankinggame.entities.AnswerEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AnswerMapper {
    public AnswerEntity toEntity(RoundId roundId, SubmittedAnswer submittedAnswer) {
        AnswerEntity answer = new AnswerEntity();
        answer.setId(submittedAnswer.answerId().value());
        answer.setRoundId(roundId.value());
        answer.setPlayerId(submittedAnswer.playerId().value());
        answer.setText(submittedAnswer.answerText().value());
        answer.setCardValue(submittedAnswer.cardValue());
        return answer;
    }

    public Map<PlayerId, SubmittedAnswer> toDomainMap(List<AnswerEntity> answerEntities) {
        return answerEntities.stream()
                .map(this::toSubmittedAnswer)
                .collect(
                    Collectors.toMap(
                            SubmittedAnswer::playerId,
                            Function.identity()
                    )
                );
    }

    public SubmittedAnswer toSubmittedAnswer(AnswerEntity entity) {
        PlayerId playerId = new PlayerId(entity.getPlayerId());
        AnswerId answerId = new AnswerId(entity.getId());
        return new SubmittedAnswer(
                playerId, answerId, new AnswerText(entity.getText()), entity.getCardValue()
        );
    }
}
