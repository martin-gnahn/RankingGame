package com.example.rankinggame.engine;

import com.example.rankinggame.engine.exceptions.AnswerTextRequiredException;
import com.example.rankinggame.engine.exceptions.AnswerTextTooLongException;
import com.example.rankinggame.engine.exceptions.InvalidCardValueException;

public record Answer(String answerText, int cardValue) {
    private static final int MAX_ANSWER_LENGTH = 500;
    private static final int MIN_CARD_VALUE = 1;
    private static final int MAX_CARD_VALUE = 10;

    public Answer {
        answerText = normalize(answerText);
        if (cardValue < MIN_CARD_VALUE || cardValue > MAX_CARD_VALUE) {
            throw new InvalidCardValueException(MIN_CARD_VALUE, MAX_CARD_VALUE);
        }
    }

    public static String normalizeText(String answerText) {
        if (answerText == null || answerText.isBlank()) {
            throw new AnswerTextRequiredException();
        }

        String trimmedAnswerText = answerText.trim();
        if (trimmedAnswerText.length() > MAX_ANSWER_LENGTH) {
            throw new AnswerTextTooLongException(MAX_ANSWER_LENGTH);
        }

        return trimmedAnswerText;
    }

    private static String normalize(String answerText) {
        return normalizeText(answerText);
    }
}
