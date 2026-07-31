package com.example.rankinggame.engine.ranking;

import com.example.rankinggame.engine.CardNumber;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RankingRulesTest {
    @Test
    void shouldReturnNoPenaltyPointsForAscendingOrder() {
        List<CardNumber> cardNumbers = List.of(
                CardNumber.of(1), CardNumber.of(2), CardNumber.of(3)
        );
        assertThat(RankingRules.countPenaltyPoints(cardNumbers)).isEqualTo(0);
    }

    @Test
    void shouldReturnMaxPenaltyPointsForDescendingOrder() {
        List<CardNumber> cardNumbers = List.of(
                CardNumber.of(3), CardNumber.of(2), CardNumber.of(1)
        );
        int numberOfTotalSteps = cardNumbers.size() - 1;
        assertThat(RankingRules.countPenaltyPoints(cardNumbers)).isEqualTo(numberOfTotalSteps);
    }

    @Test
    void shouldReturnOnePenaltyPointForTwoCardsWithSameValue() {
        List<CardNumber> cardNumbers = List.of(
                CardNumber.of(2), CardNumber.of(2)
        );
        assertThat(RankingRules.countPenaltyPoints(cardNumbers)).isEqualTo(1);
    }
}