package com.example.rankinggame.engine;

import com.example.rankinggame.engine.exceptions.AnswerAlreadySubmittedException;
import com.example.rankinggame.engine.exceptions.AnswersNotAcceptedException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class Round {
    private RoundId id;
    private RoundStatus roundStatus;
    private GameParticipant captain;
    private Question question;
    @Builder.Default
    private Map<PlayerId, SubmittedAnswer> submittedAnswers = new HashMap<>();

    public static Round start(GameParticipant captain, Question question) {
        return new Round(captain, question);
    }

    // TODO: later static Round finish(...), and setModeToSorting(...)


    private void checkIfSubmittingAnswerAllowed() {
        if (roundStatus != RoundStatus.ANSWER_SUBMISSION) {
            throw new AnswersNotAcceptedException();
        }
    }

    public boolean allAnswersSubmitted(List<GameParticipant> requiredPlayers) {
        return requiredPlayers.stream()
                .allMatch(pl -> submittedAnswers.containsKey(pl.playerId()));
    }

    public void markSortingIfAllAnswersSubmitted(List<GameParticipant> requiredPlayers) {
        boolean allPlayersHaveSubmitted = allAnswersSubmitted(requiredPlayers);
        if (allPlayersHaveSubmitted && roundStatus == RoundStatus.ANSWER_SUBMISSION) {
            roundStatus = RoundStatus.SORTING;
        }
    }

    public SubmittedAnswer submitAnswer(PlayerId playerId, String answerText, int cardValue) {
        checkIfSubmittingAnswerAllowed();
        if (submittedAnswers.containsKey(playerId)) {
            throw new AnswerAlreadySubmittedException();
        }

        SubmittedAnswer answer = new SubmittedAnswer(playerId, new AnswerText(answerText), cardValue);
        submittedAnswers.put(playerId, answer);
        return answer;
    }

    private Round(GameParticipant captain, Question question) {
        this.id = new RoundId(UUID.randomUUID());
        this.roundStatus = RoundStatus.ANSWER_SUBMISSION;
        this.captain = captain;
        this.question = question;
        this.submittedAnswers = new HashMap<>();
    }


}
