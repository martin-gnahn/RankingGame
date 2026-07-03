package com.example.rankinggame.engine;

import com.example.rankinggame.engine.exceptions.AnswerAlreadySubmittedException;
import com.example.rankinggame.engine.exceptions.AnswersNotAcceptedException;
import com.example.rankinggame.usecases.AnswerAlreadyRankedException;
import com.example.rankinggame.usecases.AnswerNotFoundException;
import com.example.rankinggame.usecases.RoundNotInSortingStateException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.*;

@Getter
@Builder
@AllArgsConstructor
public class Round {
    private RoundId id;
    private RoundStatus roundStatus;
    private GameParticipant captain;
    private Question question;
    @Builder.Default
    private List<Ranking> answerRankings = new ArrayList<>();
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

    public Ranking rankAnswer(SubmittedAnswer newAnswer) {
        ensureAnsweringIsAllowed(newAnswer);
        int oneBasedPosition = answerRankings.size() + 1;
        RankingId rankingId = new RankingId(UUID.randomUUID());
        Ranking newRanking = new Ranking(rankingId, newAnswer, oneBasedPosition);
        answerRankings.add(newRanking);
        return newRanking;
    }

    private void ensureAnsweringIsAllowed(SubmittedAnswer newAnswer) {
        if (newAnswer == null || newAnswer.answerId() == null) {
            throw new AnswerNotFoundException();
        }
        checkIfRoundIsInSortingState();
        checkIfAnswerAlreadyAddedToRanking(newAnswer);
    }

    private void checkIfAnswerAlreadyAddedToRanking(SubmittedAnswer newAnswer) {
        boolean hasAlreadyBeenRanked =
                answerRankings.stream().anyMatch(existing -> existing.getAnswer().answerId().equals(newAnswer.answerId()));
        if (hasAlreadyBeenRanked) {
            throw new AnswerAlreadyRankedException();
        }
    }

    private void checkIfRoundIsInSortingState() {
        if (roundStatus != RoundStatus.SORTING) {
            throw new RoundNotInSortingStateException();
        }
    }

    public boolean startSortingIfAllAnswersSubmitted(int submittedCount, int requiredCount) {
        if (requiredCount <= 0) {
            return false;
        }

        if (roundStatus != RoundStatus.ANSWER_SUBMISSION) {
            return false;
        }

        if (submittedCount < requiredCount) {
            return false;
        }

        roundStatus = RoundStatus.SORTING;
        return true;
    }

    public SubmittedAnswer submitAnswer(PlayerId playerId, String answerText, int cardValue) {
        checkIfSubmittingAnswerAllowed();
        if (submittedAnswers.containsKey(playerId)) {
            throw new AnswerAlreadySubmittedException();
        }

        AnswerId answerId = new AnswerId(UUID.randomUUID());
        SubmittedAnswer answer = new SubmittedAnswer(playerId, answerId, new AnswerText(answerText), cardValue);
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
