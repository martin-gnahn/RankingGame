package com.example.rankinggame.engine;

import com.example.rankinggame.engine.exceptions.AnswerAlreadySubmittedException;
import com.example.rankinggame.engine.exceptions.AnswersNotAcceptedException;
import com.example.rankinggame.usecases.*;
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
    private List<RankedAnswer> rankedAnswers = new ArrayList<>();
    @Builder.Default
    private Map<PlayerId, SubmittedAnswer> submittedAnswers = new HashMap<>();

    private Round(GameParticipant captain, Question question) {
        this.id = new RoundId(UUID.randomUUID());
        this.roundStatus = RoundStatus.ANSWER_SUBMISSION;
        this.captain = captain;
        this.question = question;
        this.submittedAnswers = new HashMap<>();
        this.rankedAnswers = new ArrayList<>();
    }

    // TODO: later static Round finish(...), and setModeToSorting(...)


    private void checkIfSubmittingAnswerAllowed() {
        if (roundStatus != RoundStatus.ANSWER_SUBMISSION) {
            throw new AnswersNotAcceptedException();
        }
    }

    public RankedAnswer rankAnswer(PlayerId playerId, AnswerId newAnswerId) {
        SubmittedAnswer newAnswer = ensureAnswerExistsInRound(newAnswerId);
        ensureRankingIsAllowed(playerId, newAnswerId);
        int oneBasedPosition = rankedAnswers.size() + 1;
        RankingId rankingId = new RankingId(UUID.randomUUID());
        RankedAnswer newRankedAnswer = new RankedAnswer(rankingId, newAnswer, oneBasedPosition);
        rankedAnswers.add(newRankedAnswer);
        return newRankedAnswer;
    }

    private SubmittedAnswer ensureAnswerExistsInRound(AnswerId newAnswerId) {
        if (newAnswerId == null) {
            throw new AnswerNotFoundException();
        }
        SubmittedAnswer newAnswer = submittedAnswers.values().stream()
                .filter(submittedAnswer -> submittedAnswer.answerId().equals(newAnswerId))
                .findFirst()
                .orElseThrow(AnswerNotPartOfRequestedRoundException::new);
        boolean isAnswerInCurrentRound = submittedAnswers.values().stream()
                .anyMatch(submittedAnswer -> submittedAnswer.answerId().equals(newAnswer.answerId()));
        if (!isAnswerInCurrentRound) {
            throw new AnswerNotPartOfRequestedRoundException();
        }
        return newAnswer;
    }

    private void ensureRankingIsAllowed(PlayerId playerId, AnswerId newAnswerId) {
        checkIfRoundIsInSortingState();
        checkIfAnswerAlreadyAddedToRanking(newAnswerId);
        checkIfPlayerIdIsFromHost(playerId);
    }

    private void checkIfPlayerIdIsFromHost(PlayerId playerId) {
        boolean playerIdIsFromHost = captain.playerId().equals(playerId);
        if (!playerIdIsFromHost) {
            throw new OnlyHostCanSortAnswers();
        }
    }

    private void checkIfAnswerAlreadyAddedToRanking(AnswerId newAnswerId) {
        boolean hasAlreadyBeenRanked =
                rankedAnswers.stream().anyMatch(existing -> existing.getAnswer().answerId().equals(newAnswerId));
        if (hasAlreadyBeenRanked) {
            throw new AnswerAlreadyRankedException();
        }
    }

    public void checkIfRoundIsInSortingState() {
        if (roundStatus != RoundStatus.SORTING) {
            throw new RoundNotInSortingStateException();
        }
    }

    static Round start(GameParticipant captain, Question question) {
        return new Round(captain, question);
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

    public boolean startRankingIfAllowed(int submittedAnswerCount, int requiredAnswerCount) {
        if (requiredAnswerCount <= 0) {
            return false;
        }

        if (roundStatus != RoundStatus.ANSWER_SUBMISSION) {
            return false;
        }

        if (submittedAnswerCount < requiredAnswerCount) {
            return false;
        }

        roundStatus = RoundStatus.SORTING;
        return true;
    }


}
