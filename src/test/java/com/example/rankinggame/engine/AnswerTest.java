package com.example.rankinggame.engine;

import com.example.rankinggame.engine.exceptions.AnswerTextRequiredException;
import com.example.rankinggame.engine.exceptions.AnswerTextTooLongException;
import com.example.rankinggame.engine.exceptions.InvalidCardValueException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnswerTest {
    @Test
    void normalizesAnswerText() {
        Answer answer = new Answer("  Good answer  ", 7);

        assertThat(answer.answerText()).isEqualTo("Good answer");
        assertThat(answer.cardValue()).isEqualTo(7);
    }

    @Test
    void rejectsBlankAnswerTextWithDomainException() {
        assertThatThrownBy(() -> new Answer(" ", 7))
                .isInstanceOf(AnswerTextRequiredException.class)
                .hasMessage("Answer text is required");
    }

    @Test
    void rejectsLongAnswerTextWithDomainException() {
        assertThatThrownBy(() -> new Answer("a".repeat(501), 7))
                .isInstanceOf(AnswerTextTooLongException.class)
                .hasMessage("Answer text must be 500 characters or fewer");
    }

    @Test
    void rejectsInvalidCardValueWithDomainException() {
        assertThatThrownBy(() -> new Answer("Good answer", 11))
                .isInstanceOf(InvalidCardValueException.class)
                .hasMessage("Card value must be between 1 and 10");
    }
}
