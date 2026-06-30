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
        AnswerText answerText = new AnswerText("  Good answer  ", 7);

        assertThat(answerText.value()).isEqualTo("Good answer");
        assertThat(answerText.cardValue()).isEqualTo(7);
    }

    @Test
    void rejectsBlankAnswerTextWithDomainException() {
        assertThatThrownBy(() -> new AnswerText(" ", 7))
                .isInstanceOf(AnswerTextRequiredException.class)
                .hasMessage("Answer text is required");
    }

    @Test
    void rejectsLongAnswerTextWithDomainException() {
        assertThatThrownBy(() -> new AnswerText("a".repeat(501), 7))
                .isInstanceOf(AnswerTextTooLongException.class)
                .hasMessage("Answer text must be 500 characters or fewer");
    }

    @Test
    void rejectsInvalidCardValueWithDomainException() {
        assertThatThrownBy(() -> new AnswerText("Good answer", 11))
                .isInstanceOf(InvalidCardValueException.class)
                .hasMessage("Card value must be between 1 and 10");
    }
}
