package com.example.rankinggame.engine.ranking;

import com.example.rankinggame.engine.CardNumber;
import com.example.rankinggame.engine.SubmittedAnswer;
import com.example.rankinggame.engine.exceptions.IncompleteRevealedRankedAnswerException;
import com.example.rankinggame.engine.exceptions.InvalidRankingPositionException;
import com.example.rankinggame.engine.exceptions.RevealedRankedAnswerField;
import lombok.Getter;

@Getter
public class RevealedRankedAnswer {
    int oneBasedPosition;
    SubmittedAnswer answer;
    CardNumber cardNumber;

    public RevealedRankedAnswer(int oneBasedPosition, SubmittedAnswer answer, CardNumber cardNumber) {
        if (oneBasedPosition < 1) {
            throw new InvalidRankingPositionException(oneBasedPosition);
        }
        if (answer == null) {
            throw new IncompleteRevealedRankedAnswerException(RevealedRankedAnswerField.SUBMITTED_ANSWER);
        }
        if (cardNumber == null) {
            throw new IncompleteRevealedRankedAnswerException(RevealedRankedAnswerField.CARD_NUMBER);
        }
        this.oneBasedPosition = oneBasedPosition;
        this.answer = answer;
        this.cardNumber = cardNumber;
    }
}
