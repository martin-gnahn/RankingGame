package com.example.rankinggame.engine.ranking;

import com.example.rankinggame.engine.CardNumber;
import com.example.rankinggame.engine.PlayerId;
import com.example.rankinggame.engine.exceptions.NegativePenaltyPointsException;
import com.example.rankinggame.engine.SubmittedAnswer;
import lombok.Getter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class RankingAssessment {
    private final int numberOfRankings;
    private final int penaltyPoints;
    private final boolean complete;

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
        List<CardNumber> allCardNumbers =
                revealedRanking.getAnswers().stream().map(RevealedRankedAnswer::getCardNumber).toList();
        return new RankingAssessment(
                revealedRanking.getAnswers().size(),
                RankingRules.countPenaltyPoints(allCardNumbers),
                isComplete(revealedRanking)
        );
    }

    private static boolean isComplete(RevealedRanking revealedRanking) {
        Set<PlayerId> rankedPlayerIds = revealedRanking.getAnswers().stream()
                .map(RevealedRankedAnswer::getAnswer)
                .map(SubmittedAnswer::playerId)
                .collect(Collectors.toUnmodifiableSet());
        return rankedPlayerIds.equals(revealedRanking.getExpectedPlayerIds());
    }
}
