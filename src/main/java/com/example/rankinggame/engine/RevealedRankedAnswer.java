package com.example.rankinggame.engine;

import com.example.rankinggame.engine.exceptions.IncompleteRevealedRankedAnswerException;
import com.example.rankinggame.engine.exceptions.InvalidRankingPositionException;
import com.example.rankinggame.engine.exceptions.RevealedRankedAnswerField;

public record RevealedRankedAnswer(
        int oneBasedPosition,
        SubmittedAnswer answer,
        CardNumber cardNumber
) {
    public RevealedRankedAnswer {
        if (oneBasedPosition < 1) {
            throw new InvalidRankingPositionException(oneBasedPosition);
        }
        if (answer == null) {
            throw new IncompleteRevealedRankedAnswerException(RevealedRankedAnswerField.SUBMITTED_ANSWER);
        }
        if (cardNumber == null) {
            throw new IncompleteRevealedRankedAnswerException(RevealedRankedAnswerField.CARD_NUMBER);
        }
    }
}
