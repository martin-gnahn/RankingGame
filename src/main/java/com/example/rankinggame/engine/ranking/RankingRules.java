package com.example.rankinggame.engine.ranking;

import com.example.rankinggame.engine.CardNumber;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.NONE)
class RankingRules {
    static int countPenaltyPoints(List<CardNumber> cardNumbers) {
        int penaltyPoints = 0;
        for (int index = 1; index < cardNumbers.size(); index++) {
            CardNumber previousCard = cardNumbers.get(index - 1);
            CardNumber currentCard = cardNumbers.get(index);
            if (!RankingRules.stepIsAllowed(previousCard, currentCard)) {
                penaltyPoints++;
            }
        }
        return penaltyPoints;
    }

    private static boolean stepIsAllowed(CardNumber previousCard, CardNumber currentCard) {
        return previousCard.value() < currentCard.value();
    }
}
