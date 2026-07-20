package com.example.rankinggame.engine;

import com.example.rankinggame.engine.exceptions.DuplicateCardValueInfoForPlayerException;
import com.example.rankinggame.engine.exceptions.MissingCardValueForRankedAnswerException;
import com.example.rankinggame.engine.exceptions.RankingAssessmentInput;
import com.example.rankinggame.engine.exceptions.RankingAssessmentInputRequiredException;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record RevealedRanking(
        List<RevealedRankedAnswer> answers,
        int expectedAnswerCount
) {
    public static RevealedRanking from(List<CardValueInfo> cardValueInfos, List<RankedAnswer> rankedAnswers) {
        ensureRequiredInput(cardValueInfos, RankingAssessmentInput.CARD_VALUE_INFOS);
        ensureRequiredInput(rankedAnswers, RankingAssessmentInput.RANKED_ANSWERS);

        CardValueLookup cardValues = CardValueLookup.from(cardValueInfos);
        List<RevealedRankedAnswer> revealedAnswers = rankedAnswers.stream()
                .sorted(Comparator.comparingInt(RankedAnswer::getOneBasedPosition))
                .map(rankedAnswer -> reveal(rankedAnswer, cardValues))
                .toList();

        return new RevealedRanking(revealedAnswers, cardValues.expectedAnswerCount());
    }

    private static void ensureRequiredInput(List<?> input, RankingAssessmentInput inputName) {
        if (input == null) {
            throw new RankingAssessmentInputRequiredException(inputName);
        }
    }

    private static RevealedRankedAnswer reveal(RankedAnswer rankedAnswer, CardValueLookup cardValues) {
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
                cardValues.cardFor(answer.playerId())
        );
    }

    private record CardValueLookup(Map<PlayerId, CardNumber> cardsByPlayerId) {
        private static CardValueLookup from(List<CardValueInfo> cardValueInfos) {
            Map<PlayerId, CardNumber> cardsByPlayerId = new HashMap<>();
            for (CardValueInfo cardValueInfo : cardValueInfos) {
                if (cardValueInfo == null) {
                    throw new RankingAssessmentInputRequiredException(RankingAssessmentInput.CARD_VALUE_INFO_ENTRIES);
                }
                CardNumber existingCard = cardsByPlayerId.putIfAbsent(cardValueInfo.playerId(), cardValueInfo.cardValue());
                if (existingCard != null) {
                    throw new DuplicateCardValueInfoForPlayerException(cardValueInfo.playerId());
                }
            }
            return new CardValueLookup(cardsByPlayerId);
        }

        private CardNumber cardFor(PlayerId playerId) {
            CardNumber cardNumber = cardsByPlayerId.get(playerId);
            if (cardNumber == null) {
                throw new MissingCardValueForRankedAnswerException(playerId);
            }
            return cardNumber;
        }

        private int expectedAnswerCount() {
            return cardsByPlayerId.size();
        }
    }
}
