package com.example.rankinggame.engine;

import com.example.rankinggame.engine.exceptions.AnswerTextRequiredException;
import com.example.rankinggame.engine.exceptions.AnswerTextTooLongException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnswerTest {
    @Test
    void normalizesAnswerText() {
        AnswerText answerText = new AnswerText("  Good answer  ");

        assertThat(answerText.value()).isEqualTo("Good answer");
    }

    @Test
    void submittedAnswerKeepsPlayerAnswerTextAndCardValue() {
        PlayerId playerId = new PlayerId(UUID.randomUUID());
        AnswerId answerId = new AnswerId(UUID.randomUUID());
        SubmittedAnswer submittedAnswer = new SubmittedAnswer(
                playerId,
                answerId,
                new AnswerText("  Good answer  ")
        );

        assertThat(submittedAnswer.playerId()).isEqualTo(playerId);
        assertThat(submittedAnswer.answerText().value()).isEqualTo("Good answer");
    }

    @Test
    void rejectsBlankAnswerTextWithDomainException() {
        assertThatThrownBy(() -> new AnswerText(" "))
                .isInstanceOf(AnswerTextRequiredException.class)
                .hasMessage("Answer text is required");
    }

    @Test
    void rejectsLongAnswerTextWithDomainException() {
        assertThatThrownBy(() -> new AnswerText("a".repeat(501)))
                .isInstanceOf(AnswerTextTooLongException.class)
                .hasMessage("Answer text must be 500 characters or fewer");
    }
}
