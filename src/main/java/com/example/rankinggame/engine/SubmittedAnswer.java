package com.example.rankinggame.engine;

import com.example.rankinggame.engine.exceptions.InvalidCardValueException;

public record SubmittedAnswer(PlayerId playerId, AnswerText answerText, int cardValue) {
    private static final int MAX_ANSWER_LENGTH = 500;
    private static final int MIN_CARD_VALUE = 1;
    private static final int MAX_CARD_VALUE = 10;

    public SubmittedAnswer(PlayerId playerId, AnswerText answerText, int cardValue) {
        if (cardValue < MIN_CARD_VALUE || cardValue > MAX_CARD_VALUE) {
            throw new InvalidCardValueException(MIN_CARD_VALUE, MAX_CARD_VALUE);
        }
        this.playerId = playerId;
        this.answerText = answerText;
        this.cardValue = cardValue;
    }
}
