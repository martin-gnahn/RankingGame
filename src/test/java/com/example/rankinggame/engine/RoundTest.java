package com.example.rankinggame.engine;

import com.example.rankinggame.engine.exceptions.AnswerAlreadySubmittedException;
import com.example.rankinggame.usecases.AnswerAlreadyRankedException;
import com.example.rankinggame.usecases.AnswerNotPartOfRequestedRoundException;
import com.example.rankinggame.usecases.OnlyHostCanSortAnswers;
import com.example.rankinggame.usecases.RoundNotInSortingStateException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class RoundTest {

    private static final String CAPTAIN_PLAYER_NAME = "Player1";
    private static final String GUEST_PLAYER_NAME = "Player2";
    private static final UUID CAPTAIN_PLAYER_ID = UUID.randomUUID();
    private static final UUID GUEST_PLAYER_ID = UUID.randomUUID();
    private static final UUID QUESTION_ID = UUID.randomUUID();
    private static final String QUESTION_TEXT = "Is Java good?";
    private static final String QUESTION_CATEGORY = "default";
    private static final String ANSWER1 = "Answer1";
    private static final String ANSWER2 = "Answer2";

    @Test
    void newRoundStartsInAnswerSubmissionStateWithCaptain() {
        RoundTestContext context = newRoundContext(false);

        RoundStatus roundStatus = context.round().getRoundStatus();
        assertThat(roundStatus).isEqualTo(RoundStatus.ANSWER_SUBMISSION);
        assertThat(context.round().getCaptain().name()).isEqualTo(CAPTAIN_PLAYER_NAME);
    }

    @Test
    void playersCanSubmitAnswersDuringAnswerSubmission() {
        RoundTestContext context = newRoundContext(true);

        AnswerTestContext answerTestContext = context.answerTestContext();
        SubmittedAnswer firstAnswer = answerTestContext.submittedAnswers().getFirst();
        SubmittedAnswer secondAnswer = answerTestContext.submittedAnswers().get(1);

        Map<PlayerId, SubmittedAnswer> submittedAnswers = context.round().getSubmittedAnswers();
        assertThat(submittedAnswers)
                .contains(
                        entry(context.captain().playerId(), firstAnswer),
                        entry(context.guest().playerId(), secondAnswer)
                );
    }

    @Test
    void captainCanRankSubmittedAnswersInChosenOrder() {
        RoundTestContext context = newRoundContext(true);
        AnswerTestContext answerTestContext = context.answerTestContext();

        int submittedAnswerCount = answerTestContext.submittedAnswers().size();
        GameParticipant captain = context.captain();
        int requiredAnswerCount = List.of(captain, context.guest()).size();

        context.round().startRankingIfAllowed(submittedAnswerCount, requiredAnswerCount);


        SubmittedAnswer firstAnswer = answerTestContext.submittedAnswers().getFirst();
        SubmittedAnswer secondAnswer = answerTestContext.submittedAnswers().get(1);

        context.round().rankAnswer(captain.playerId(), secondAnswer.answerId());
        context.round().rankAnswer(captain.playerId(), firstAnswer.answerId());

        List<RankedAnswer> rankedAnswers = context.round().getRankedAnswers();
        assertThat(rankedAnswers).extracting(RankedAnswer::getAnswer)
                .extracting(SubmittedAnswer::answerText)
                .extracting(AnswerText::value)
                .containsExactly(ANSWER2, ANSWER1);
    }

    @Test
    void samePlayerCannotSubmitTwoAnswers() {
        RoundTestContext context = newRoundContext(true);
        assertThatExceptionOfType(AnswerAlreadySubmittedException.class)
                .isThrownBy(() -> context.round().submitAnswer(new PlayerId(CAPTAIN_PLAYER_ID), "Answer3"));
    }

    @Test
    void guestCannotRankAnswer() {
        RoundTestContext context = newRoundContext(true);
        AnswerTestContext answerTestContext = context.answerTestContext();
        startRanking(context);

        SubmittedAnswer firstAnswer = answerTestContext.submittedAnswers().getFirst();

        assertThatExceptionOfType(OnlyHostCanSortAnswers.class)
                .isThrownBy(() -> context.round().rankAnswer(context.guest().playerId(), firstAnswer.answerId()));
    }

    @Test
    void cannotRankAnswerBeforeSorting() {
        RoundTestContext context = newRoundContext(true);
        SubmittedAnswer firstAnswer = context.answerTestContext().submittedAnswers().getFirst();

        assertThatExceptionOfType(RoundNotInSortingStateException.class)
                .isThrownBy(() -> context.round().rankAnswer(context.captain().playerId(), firstAnswer.answerId()));
    }

    @Test
    void sameAnswerCannotBeRankedTwice() {
        RoundTestContext context = newRoundContext(true);
        AnswerTestContext answerTestContext = context.answerTestContext();
        startRanking(context);

        SubmittedAnswer firstAnswer = answerTestContext.submittedAnswers().getFirst();
        context.round().rankAnswer(context.captain().playerId(), firstAnswer.answerId());

        assertThatExceptionOfType(AnswerAlreadyRankedException.class)
                .isThrownBy(() -> context.round().rankAnswer(context.captain().playerId(), firstAnswer.answerId()));
    }

    @Test
    void unknownAnswerCannotBeRanked() {
        RoundTestContext context = newRoundContext(true);
        PlayerId playerId = new PlayerId(UUID.randomUUID());
        AnswerId answerId = new AnswerId(UUID.randomUUID());
        AnswerText unknownAnswerText = new AnswerText("UnknownAnswer");
        SubmittedAnswer unknownAnswer = new SubmittedAnswer(playerId, answerId, unknownAnswerText);

        assertThatExceptionOfType(AnswerNotPartOfRequestedRoundException.class)
                .isThrownBy(() -> context.round().rankAnswer(context.captain().playerId(), unknownAnswer.answerId()));
    }

    private void startRanking(RoundTestContext context) {
        int submittedAnswerCount = context.answerTestContext().submittedAnswers().size();
        int requiredAnswerCount = List.of(context.captain(), context.guest()).size();
        context.round().startRankingIfAllowed(submittedAnswerCount, requiredAnswerCount);
    }

    private RoundTestContext newRoundContext(boolean submitAnswers) {
        GameParticipant captain = new GameParticipant(new PlayerId(CAPTAIN_PLAYER_ID), CAPTAIN_PLAYER_NAME);
        GameParticipant guest = new GameParticipant(new PlayerId(GUEST_PLAYER_ID), GUEST_PLAYER_NAME);
        Question firstQuestion = new Question(new QuestionId(QUESTION_ID), QUESTION_TEXT, QUESTION_CATEGORY);
        Round round = Round.start(captain, firstQuestion);

        final AnswerTestContext answerTestContext = new AnswerTestContext();
        if (submitAnswers) {
            var submittedAnswer1 = round.submitAnswer(captain.playerId(), ANSWER1);
            var submittedAnswer2 = round.submitAnswer(guest.playerId(), ANSWER2);
            answerTestContext.submittedAnswers().addAll(List.of(submittedAnswer1, submittedAnswer2));
        }
        return new RoundTestContext(captain, guest, round, answerTestContext);
    }

    private record RoundTestContext(GameParticipant captain, GameParticipant guest, Round round,
                                    AnswerTestContext answerTestContext) {
    }

    private record AnswerTestContext(List<SubmittedAnswer> submittedAnswers) {
        public AnswerTestContext() {
            this(new ArrayList<>());
        }
    }
}
