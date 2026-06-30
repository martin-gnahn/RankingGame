package com.example.rankinggame.mapper;

import com.example.rankinggame.engine.Answer;
import com.example.rankinggame.engine.PlayerId;
import com.example.rankinggame.engine.RoundId;
import com.example.rankinggame.entities.AnswerEntity;
import com.example.rankinggame.usecases.SubmitAnswerService;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnswerMapper {
    public AnswerEntity toEntity(RoundId roundId, PlayerId playerId, Answer submittedAnswer) {
        AnswerEntity answer = new AnswerEntity();
        answer.setRoundId(roundId.value());
        answer.setPlayerId(playerId.value());
        answer.setText(submittedAnswer.answerText());
        answer.setCardValue(submittedAnswer.cardValue());
        return answer;
    }

    public Map<PlayerId, Answer> toDomainMap(List<AnswerEntity> answerEntities) {
        return answerEntities.stream()
                .map(this::toAnswerWithPlayerId)
                .collect(
                    Collectors.toMap(
                            SubmittedAnswer::playerId,
                            this::toAnswerWithoutPlayerId
                    )
                );
    }

    private Answer toAnswerWithoutPlayerId(SubmittedAnswer submittedAnswer) {
        return new Answer(submittedAnswer.text(), submittedAnswer.cardValue());
    }

    private SubmittedAnswer toAnswerWithPlayerId(AnswerEntity entity) {
        PlayerId playerId = new PlayerId(entity.getPlayerId());
        return new SubmittedAnswer(
                playerId, entity.getText(), entity.getCardValue()
        );
    }

    private record SubmittedAnswer(PlayerId playerId, String text, int cardValue) {
    }
}
