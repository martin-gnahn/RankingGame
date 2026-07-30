package com.example.rankinggame.engine.ranking;

import com.example.rankinggame.engine.PlayerId;
import com.example.rankinggame.engine.RankedAnswer;
import com.example.rankinggame.engine.SubmittedAnswer;
import com.example.rankinggame.engine.exceptions.DuplicateRankedAnswerForPlayerException;
import com.example.rankinggame.engine.exceptions.DuplicateRankingPositionException;
import com.example.rankinggame.engine.exceptions.RankingAssessmentInput;
import com.example.rankinggame.engine.exceptions.RankingAssessmentInputRequiredException;
import com.example.rankinggame.engine.exceptions.UnexpectedRankingPositionException;
import lombok.Getter;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public class RevealedRanking {
    private final List<RevealedRankedAnswer> answers;
    private final Set<PlayerId> expectedPlayerIds;

    private RevealedRanking(List<RevealedRankedAnswer> answers, Set<PlayerId> expectedPlayerIds) {
        this.answers = List.copyOf(answers);
        this.expectedPlayerIds = Set.copyOf(expectedPlayerIds);
    }

    public static RevealedRanking reveal(List<CardValueInfo> cardValueInfos, List<RankedAnswer> rankedAnswers) {
        ensureRequiredInput(cardValueInfos, RankingAssessmentInput.CARD_VALUE_INFOS);
        ensureRequiredInput(rankedAnswers, RankingAssessmentInput.RANKED_ANSWERS);

        PlayerToCardMap playerToCardMap = PlayerToCardMap.from(cardValueInfos);
        validateRankingEntries(rankedAnswers);
        List<RevealedRankedAnswer> revealedAnswers = rankedAnswers.stream()
                .sorted(Comparator.comparingInt(RankedAnswer::getOneBasedPosition))
                .map(rankedAnswer -> RevealedRanking.reveal(rankedAnswer, playerToCardMap))
                .toList();

        return new RevealedRanking(revealedAnswers, playerToCardMap.expectedPlayerIds());
    }

    public RankingAssessment assess() {
        return RankingAssessment.from(this);
    }

    private static void ensureRequiredInput(List<?> input, RankingAssessmentInput inputName) {
        if (input == null) {
            throw new RankingAssessmentInputRequiredException(inputName);
        }
    }

    private static void validateRankingEntries(List<RankedAnswer> rankedAnswers) {
        ensureCompleteEntries(rankedAnswers);
        ensureUniquePlayers(rankedAnswers);
        ensureContinuousPositions(rankedAnswers);
    }

    private static void ensureCompleteEntries(List<RankedAnswer> rankedAnswers) {
        for (RankedAnswer rankedAnswer : rankedAnswers) {
            if (rankedAnswer == null) {
                throw new RankingAssessmentInputRequiredException(
                        RankingAssessmentInput.RANKED_ANSWER_ENTRIES
                );
            }
            if (rankedAnswer.getAnswer() == null) {
                throw new RankingAssessmentInputRequiredException(
                        RankingAssessmentInput.SUBMITTED_ANSWERS_INSIDE_RANKED_ANSWERS
                );
            }
        }
    }

    private static void ensureUniquePlayers(List<RankedAnswer> rankedAnswers) {
        Set<PlayerId> playerIds = new HashSet<>();

        for (RankedAnswer rankedAnswer : rankedAnswers) {
            PlayerId playerId = rankedAnswer.getAnswer().playerId();
            if (!playerIds.add(playerId)) {
                throw new DuplicateRankedAnswerForPlayerException(playerId);
            }
        }
    }

    private static void ensureContinuousPositions(List<RankedAnswer> rankedAnswers) {
        List<Integer> positions = rankedAnswers.stream()
                .map(RankedAnswer::getOneBasedPosition)
                .sorted()
                .toList();

        for (int index = 0; index < positions.size(); index++) {
            int expectedPosition = index + 1;
            int actualPosition = positions.get(index);

            if (actualPosition < expectedPosition) {
                throw new DuplicateRankingPositionException(actualPosition);
            }
            if (actualPosition > expectedPosition) {
                throw new UnexpectedRankingPositionException(expectedPosition, actualPosition);
            }
        }
    }

    private static RevealedRankedAnswer reveal(RankedAnswer rankedAnswer, PlayerToCardMap playerToCardMap) {
        SubmittedAnswer answer = rankedAnswer.getAnswer();
        return new RevealedRankedAnswer(
                rankedAnswer.getOneBasedPosition(),
                answer,
                playerToCardMap.cardFor(answer.playerId())
        );
    }
}
