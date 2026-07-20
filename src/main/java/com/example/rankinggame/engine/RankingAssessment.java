package com.example.rankinggame.engine;

import com.example.rankinggame.engine.exceptions.NegativePenaltyPointsException;

import java.util.List;

public record RankingAssessment(
        int penaltyPoints,
        boolean complete
) {
    public RankingAssessment {
        if (penaltyPoints < 0) {
            throw new NegativePenaltyPointsException(penaltyPoints);
        }
    }

    public boolean isPerfect() {
        return complete && penaltyPoints == 0;
    }

    public boolean isComplete() {
        return complete;
    }

    public static RankingAssessment from(RevealedRanking revealedRanking) {
        return new RankingAssessment(
                countDescendingSteps(revealedRanking.answers()),
                isComplete(revealedRanking)
        );
    }

    private static int countDescendingSteps(List<RevealedRankedAnswer> revealedAnswers) {
        int penaltyPoints = 0;
        for (int index = 1; index < revealedAnswers.size(); index++) {
            if (cardValueAt(revealedAnswers, index) < cardValueAt(revealedAnswers, index - 1)) {
                penaltyPoints++;
            }
        }
        return penaltyPoints;
    }

    private static int cardValueAt(List<RevealedRankedAnswer> revealedAnswers, int index) {
        return revealedAnswers.get(index).cardNumber().value();
    }

    private static boolean isComplete(RevealedRanking revealedRanking) {
        long rankedPlayerCount = revealedRanking.answers().stream()
                .map(RevealedRankedAnswer::answer)
                .map(SubmittedAnswer::playerId)
                .distinct()
                .count();
        return rankedPlayerCount == revealedRanking.expectedAnswerCount();
    }
}
