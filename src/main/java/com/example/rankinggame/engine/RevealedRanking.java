package com.example.rankinggame.engine;

import com.example.rankinggame.engine.exceptions.RankingAssessmentInput;
import com.example.rankinggame.engine.exceptions.RankingAssessmentInputRequiredException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RevealedRanking {
    public List<RevealedRankedAnswer> answers;
    public Set<PlayerId> expectedPlayerIds;

    public static RevealedRanking reveal(List<CardValueInfo> cardValueInfos, List<RankedAnswer> rankedAnswers) {
        ensureRequiredInput(cardValueInfos, RankingAssessmentInput.CARD_VALUE_INFOS);
        ensureRequiredInput(rankedAnswers, RankingAssessmentInput.RANKED_ANSWERS);

        PlayerToCardMap playerToCardMap = PlayerToCardMap.from(cardValueInfos);
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

    private static RevealedRankedAnswer reveal(RankedAnswer rankedAnswer, PlayerToCardMap playerToCardMap) {
        if (rankedAnswer == null) {
            throw new RankingAssessmentInputRequiredException(RankingAssessmentInput.RANKED_ANSWER_ENTRIES);
        }
        SubmittedAnswer answer = rankedAnswer.getAnswer();
        if (answer == null) {
            throw new RankingAssessmentInputRequiredException(RankingAssessmentInput.SUBMITTED_ANSWERS_INSIDE_RANKED_ANSWERS);
        }
        return new RevealedRankedAnswer(
                rankedAnswer.getOneBasedPosition(),
                answer,
                playerToCardMap.cardFor(answer.playerId())
        );
    }
}
