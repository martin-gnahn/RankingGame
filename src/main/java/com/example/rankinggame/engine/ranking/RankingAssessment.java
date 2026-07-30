package com.example.rankinggame.engine.ranking;

import com.example.rankinggame.engine.PlayerId;
import com.example.rankinggame.engine.exceptions.NegativePenaltyPointsException;
import com.example.rankinggame.engine.SubmittedAnswer;
import lombok.Getter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class RankingAssessment {
    public int numberOfRankings;
    public int penaltyPoints;
    public boolean complete;

    private RankingAssessment(int numberOfRankings, int penaltyPoints, boolean complete) {
        if (penaltyPoints < 0) {
            throw new NegativePenaltyPointsException(penaltyPoints);
        }
        this.numberOfRankings = numberOfRankings;
        this.penaltyPoints = penaltyPoints;
        this.complete = complete;
    }

    public boolean isPerfect() {
        return complete && penaltyPoints == 0;
    }

    public boolean isEmpty() {
        return numberOfRankings == 0;
    }

    public static RankingAssessment from(RevealedRanking revealedRanking) {
        return new RankingAssessment(
                revealedRanking.getAnswers().size(),
                countDescendingSteps(revealedRanking.getAnswers()),
                isComplete(revealedRanking)
        );
    }

    private static int countDescendingSteps(List<RevealedRankedAnswer> revealedAnswers) {
        int penaltyPoints = 0;
        for (int index = 1; index < revealedAnswers.size(); index++) {
            int previousCardValue = cardValueAt(revealedAnswers, index - 1);
            int currentCardValue = cardValueAt(revealedAnswers, index);
            if (previousCardValue > currentCardValue) {
                penaltyPoints++;
            }
        }
        return penaltyPoints;
    }

    private static int cardValueAt(List<RevealedRankedAnswer> revealedAnswers, int index) {
        return revealedAnswers.get(index).getCardNumber().value();
    }

    private static boolean isComplete(RevealedRanking revealedRanking) {
        Set<PlayerId> rankedPlayerIds = revealedRanking.getAnswers().stream()
                .map(RevealedRankedAnswer::getAnswer)
                .map(SubmittedAnswer::playerId)
                .collect(Collectors.toUnmodifiableSet());
        return rankedPlayerIds.equals(revealedRanking.getExpectedPlayerIds());
    }
}
