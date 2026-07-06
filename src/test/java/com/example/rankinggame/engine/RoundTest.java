package com.example.rankinggame.engine;

import com.example.rankinggame.engine.exceptions.AnswerAlreadySubmittedException;
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
    void checkIfGameCanBeStarted() {
        GameTestContext context = setupGameAndGetContext(false);

        RoundStatus roundStatus = context.firstRound().getRoundStatus();
        assertThat(roundStatus).isEqualTo(RoundStatus.ANSWER_SUBMISSION);
        assertThat(context.firstRound().getCaptain().name()).isEqualTo(CAPTAIN_PLAYER_NAME);
    }

    @Test
    void checkIfPlayersCanSubmitAnswers() {
        GameTestContext context = setupGameAndGetContext(true);

        AnswerTestContext answerTestContext = context.answerTestContext();
        SubmittedAnswer firstAnswer = answerTestContext.submittedAnswers().getFirst();
        SubmittedAnswer secondAnswer = answerTestContext.submittedAnswers().get(1);

        Map<PlayerId, SubmittedAnswer> submittedAnswers = context.firstRound().getSubmittedAnswers();
        assertThat(submittedAnswers)
                .contains(
                        entry(context.captainPlayer().playerId(), firstAnswer),
                        entry(context.guestPlayer().playerId(), secondAnswer)
                );
    }

    @Test
    void checkIfHostCanRankAnswersCorrectly() {
        GameTestContext context = setupGameAndGetContext(true);
        AnswerTestContext answerTestContext = context.answerTestContext();

        int submittedAnswerCount = answerTestContext.submittedAnswers().size();
        GameParticipant captainPlayer = context.captainPlayer();
        int requiredAnswerCount = List.of(captainPlayer, context.guestPlayer()).size();

        context.firstRound().startRankingIfAllowed(submittedAnswerCount, requiredAnswerCount);


        SubmittedAnswer firstAnswer = answerTestContext.submittedAnswers().getFirst();
        SubmittedAnswer secondAnswer = answerTestContext.submittedAnswers().get(1);

        context.firstRound().rankAnswer(captainPlayer.playerId(), secondAnswer.answerId());
        context.firstRound().rankAnswer(captainPlayer.playerId(), firstAnswer.answerId());

        List<RankedAnswer> rankedAnswers = context.firstRound().getRankedAnswers();
        assertThat(rankedAnswers).extracting(RankedAnswer::getAnswer)
                .extracting(SubmittedAnswer::answerText)
                .extracting(AnswerText::value)
                .containsExactly(ANSWER2, ANSWER1);
    }

    @Test
    void shouldThrowAnswerAlreadySubmittedErrorIfPlayerSentTwoAnswers() {
        GameTestContext context = setupGameAndGetContext(true);
        assertThatExceptionOfType(AnswerAlreadySubmittedException.class)
                .isThrownBy(() -> context.firstRound().submitAnswer(new PlayerId(CAPTAIN_PLAYER_ID), "Answer3", 3));
    }

    private GameTestContext setupGameAndGetContext(boolean submitAnswers) {
        GameParticipant captainPlayer = new GameParticipant(new PlayerId(CAPTAIN_PLAYER_ID), CAPTAIN_PLAYER_NAME);
        GameParticipant guestPlayer = new GameParticipant(new PlayerId(GUEST_PLAYER_ID), GUEST_PLAYER_NAME);
        List<GameParticipant> players = List.of(captainPlayer, guestPlayer);
        Game game = new Game(players);
        Question firstQuestion = new Question(new QuestionId(QUESTION_ID), QUESTION_TEXT, QUESTION_CATEGORY);
        game.start(captainPlayer, firstQuestion);
        Round firstRound = game.getCurrentRound();

        final AnswerTestContext answerTestContext = new AnswerTestContext();
        if (submitAnswers) {
            var submittedAnswer1 = firstRound.submitAnswer(captainPlayer.playerId(), ANSWER1, 1);
            var submittedAnswer2 = firstRound.submitAnswer(guestPlayer.playerId(), ANSWER2, 2);
            answerTestContext.submittedAnswers().addAll(List.of(submittedAnswer1, submittedAnswer2));
        }
        return new GameTestContext(captainPlayer, guestPlayer, firstRound, answerTestContext);
    }

    private record GameTestContext(GameParticipant captainPlayer, GameParticipant guestPlayer, Round firstRound,
                                   AnswerTestContext answerTestContext) {
    }

    private record AnswerTestContext(List<SubmittedAnswer> submittedAnswers) {
        public AnswerTestContext() {
            this(new ArrayList<>());
        }
    }
}
