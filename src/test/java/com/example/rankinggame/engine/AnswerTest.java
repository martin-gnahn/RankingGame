package com.example.rankinggame.engine;

import com.example.rankinggame.engine.exceptions.AnswerTextRequiredException;
import com.example.rankinggame.engine.exceptions.AnswerTextTooLongException;
import com.example.rankinggame.engine.exceptions.InvalidCardValueException;
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
                new AnswerText("  Good answer  "),
                7
        );

        assertThat(submittedAnswer.playerId()).isEqualTo(playerId);
        assertThat(submittedAnswer.answerText().value()).isEqualTo("Good answer");
        assertThat(submittedAnswer.cardValue()).isEqualTo(7);
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

    @Test
    void rejectsInvalidCardValueWithDomainException() {
        assertThatThrownBy(() -> new SubmittedAnswer(
                new PlayerId(UUID.randomUUID()),
                new AnswerId(UUID.randomUUID()),
                new AnswerText("Good answer"),
                11
        ))
                .isInstanceOf(InvalidCardValueException.class)
                .hasMessage("Card value must be between 1 and 10");
    }
}
