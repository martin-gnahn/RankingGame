package com.example.rankinggame.engine;

import com.example.rankinggame.engine.exceptions.DuplicateCardValueInfoForPlayerException;
import com.example.rankinggame.engine.exceptions.InvalidRankingPositionException;
import com.example.rankinggame.engine.exceptions.MissingCardValueForRankedAnswerException;
import com.example.rankinggame.engine.ranking.CardValueInfo;
import com.example.rankinggame.engine.ranking.RankingAssessment;
import com.example.rankinggame.engine.ranking.RevealedRankedAnswer;
import com.example.rankinggame.engine.ranking.RevealedRanking;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RankingAssessmentTest {
    private final RoundId roundId = new RoundId(UUID.randomUUID());
    private final PlayerId playerWithCard1 = new PlayerId(UUID.randomUUID());
    private final PlayerId playerWithCard2 = new PlayerId(UUID.randomUUID());
    private final PlayerId playerWithCard3 = new PlayerId(UUID.randomUUID());

    @Test
    void returnsNoPenaltyPointsWhenRevealedCardValuesAreAscending() {
        RankingAssessment assessment = assess(rankedAnswers(playerWithCard1, playerWithCard2, playerWithCard3));

        assertThat(assessment.isPerfect()).isTrue();
        assertThat(assessment.isComplete()).isTrue();
        assertThat(assessment.getPenaltyPoints()).isZero();
    }

    @Test
    void returnsOnePenaltyPointWhenRevealedCardValuesDropOnce() {
        RankingAssessment assessment = assess(rankedAnswers(playerWithCard2, playerWithCard3, playerWithCard1));

        assertThat(assessment.isPerfect()).isFalse();
        assertThat(assessment.isComplete()).isTrue();
        assertThat(assessment.getPenaltyPoints()).isEqualTo(1);
    }

    @Test
    void returnsTwoPenaltyPointsWhenRevealedCardValuesDropTwice() {
        RankingAssessment assessment = assess(rankedAnswers(playerWithCard3, playerWithCard2, playerWithCard1));

        assertThat(assessment.isPerfect()).isFalse();
        assertThat(assessment.isComplete()).isTrue();
        assertThat(assessment.getPenaltyPoints()).isEqualTo(2);
    }

    @Test
    void evaluatesRankingByOneBasedPositionBeforeCheckinggetPenaltyPoints() {
        List<RankedAnswer> shuffledRankedAnswers = List.of(
                rankedAnswer(2, playerWithCard2),
                rankedAnswer(1, playerWithCard1),
                rankedAnswer(3, playerWithCard3)
        );

        RankingAssessment assessment = assess(shuffledRankedAnswers);

        assertThat(assessment.isPerfect()).isTrue();
        assertThat(assessment.isComplete()).isTrue();
        assertThat(assessment.getPenaltyPoints()).isZero();
    }

    @Test
    void returnsIncompleteAssessmentWhenSomeAnswersAreNotRankedYet() {
        RankingAssessment assessment = assess(List.of(
                rankedAnswer(1, playerWithCard1),
                rankedAnswer(2, playerWithCard2)
        ));

        assertThat(assessment.isComplete()).isFalse();
        assertThat(assessment.isPerfect()).isFalse();
        assertThat(assessment.getPenaltyPoints()).isZero();
    }

    @Test
    void rejectsRankedAnswerWithoutMatchingCardValue() {
        PlayerId playerWithoutCard = new PlayerId(UUID.randomUUID());

        assertThatThrownBy(() -> reveal(rankedAnswers(playerWithCard1, playerWithCard2, playerWithoutCard)))
                .isInstanceOf(MissingCardValueForRankedAnswerException.class);
    }

    @Test
    void rejectsDuplicateCardValueInfoForSamePlayer() {
        List<CardValueInfo> duplicatePlayerCardValues = List.of(
                new CardValueInfo(playerWithCard1, CardNumber.of(1)),
                new CardValueInfo(playerWithCard1, CardNumber.of(2))
        );

        assertThatThrownBy(() -> RevealedRanking.reveal(duplicatePlayerCardValues, rankedAnswers(playerWithCard1, playerWithCard2, playerWithCard3)))
                .isInstanceOf(DuplicateCardValueInfoForPlayerException.class);
    }

    @Test
    void revealedRankedAnswerRequiresPositivePosition() {
        assertThatThrownBy(() -> new RevealedRankedAnswer(0, submittedAnswer(playerWithCard1, 1), CardNumber.of(1)))
                .isInstanceOf(InvalidRankingPositionException.class);
    }

    private RankingAssessment assess(List<RankedAnswer> rankedAnswers) {
        return RankingAssessment.from(reveal(rankedAnswers));
    }

    private RevealedRanking reveal(List<RankedAnswer> rankedAnswers) {
        return RevealedRanking.reveal(cardValues(), rankedAnswers);
    }

    private List<CardValueInfo> cardValues() {
        return List.of(
                new CardValueInfo(playerWithCard1, CardNumber.of(1)),
                new CardValueInfo(playerWithCard2, CardNumber.of(2)),
                new CardValueInfo(playerWithCard3, CardNumber.of(3))
        );
    }

    private List<RankedAnswer> rankedAnswers(PlayerId firstPlayer, PlayerId secondPlayer, PlayerId thirdPlayer) {
        return List.of(
                rankedAnswer(1, firstPlayer),
                rankedAnswer(2, secondPlayer),
                rankedAnswer(3, thirdPlayer)
        );
    }

    private RankedAnswer rankedAnswer(int oneBasedPosition, PlayerId playerId) {
        return new RankedAnswer(new RankingId(UUID.randomUUID()), submittedAnswer(playerId, oneBasedPosition), oneBasedPosition);
    }

    private SubmittedAnswer submittedAnswer(PlayerId playerId, int oneBasedPosition) {
        return new SubmittedAnswer(
                playerId,
                new AnswerId(UUID.randomUUID()),
                new AnswerText("Answer " + oneBasedPosition)
        );
    }
}
