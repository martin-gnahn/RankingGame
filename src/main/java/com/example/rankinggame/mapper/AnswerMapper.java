package com.example.rankinggame.mapper;

import com.example.rankinggame.engine.AnswerText;
import com.example.rankinggame.engine.PlayerId;
import com.example.rankinggame.engine.RoundId;
import com.example.rankinggame.engine.SubmittedAnswer;
import com.example.rankinggame.entities.AnswerEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnswerMapper {
    public AnswerEntity toEntity(RoundId roundId, PlayerId playerId, SubmittedAnswer submittedAnswer) {
        AnswerEntity answer = new AnswerEntity();
        answer.setRoundId(roundId.value());
        answer.setPlayerId(playerId.value());
        answer.setText(submittedAnswer.answerText().value());
        answer.setCardValue(submittedAnswer.cardValue());
        return answer;
    }

    public Map<PlayerId, AnswerText> toDomainMap(List<AnswerEntity> answerEntities) {
        return answerEntities.stream()
                .map(this::toSubmittedAnswer)
                .collect(
                    Collectors.toMap(
                            SubmittedAnswer::playerId,
                            this::toAnswerText
                    )
                );
    }

    private AnswerText toAnswerText(SubmittedAnswer submittedAnswer) {
        return new AnswerText(submittedAnswer.answerText().value());
    }

    private SubmittedAnswer toSubmittedAnswer(AnswerEntity entity) {
        PlayerId playerId = new PlayerId(entity.getPlayerId());
        return new SubmittedAnswer(
                playerId, new AnswerText(entity.getText()), entity.getCardValue()
        );
    }
}
