package com.example.rankinggame.integration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Backbone only: add setup and assertions when SortAnswerController/SortAnswerService behavior is implemented.")
class SortAnswerControllerIntegrationTest extends BackendIntegrationTest {

    @Test
    void hostCanRankSubmittedAnswersAndReadThemBackInRankingOrder() {
    }

    @Test
    void rankingMultipleAnswersAssignsConsecutivePositions() {
    }

    @Test
    void rankingTheSameAnswerTwiceIsRejected() {
    }

    @Test
    void guestCannotRankSubmittedAnswers() {
    }

    @Test
    void answerFromAnotherRoundCannotBeRanked() {
    }

    @Test
    void answersCannotBeRankedBeforeRoundIsInSortingState() {
    }

    @Test
    void readingRankingForRoundWithoutRankedAnswersReturnsEmptyOrder() {
    }
}
