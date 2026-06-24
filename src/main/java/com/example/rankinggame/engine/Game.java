package com.example.rankinggame.engine;

import com.example.rankinggame.engine.exceptions.CannotUseSameQuestionAgainException;
import com.example.rankinggame.engine.exceptions.CaptainNotFoundException;
import com.example.rankinggame.engine.exceptions.GameCannotBeStartedException;
import com.example.rankinggame.engine.exceptions.InvalidPlayerException;
import com.example.rankinggame.engine.exceptions.NoPlayerInGameException;
import com.example.rankinggame.engine.exceptions.NotEnoughPlayersException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// TODO: this is a POJO for Game
@Data
@Builder
@AllArgsConstructor
public class Game {
    public static final int REQUIRED_NUMBER_OF_PARTICIPANTS = 2;
    // final GameId gameId;
    private List<GameParticipant> participants;
    boolean isActive;
    @Builder.Default
    private List<Round> allRounds = new ArrayList<>();
    private GameStatus status;

    // Uses the persisted one-based round number; convert to a list index at the boundary.
    private int currentRoundNumber;

    public Round getCurrentRound() {
        return allRounds.get(currentRoundNumber - 1);
    }

    public Game(List<GameParticipant> participants) {
        // this.gameId = new GameId(UUID.randomUUID());
        this.participants = participants;
        this.allRounds = new ArrayList<>();
        this.status = GameStatus.WAITING;
    }

    public boolean hasEnoughPlayers() {
        //noinspection SizeReplaceableByIsEmpty
        return participants.size() >= REQUIRED_NUMBER_OF_PARTICIPANTS;
    }

    public boolean isActive() {
        return status == GameStatus.IN_PROGRESS;
    }

    public void start(Question firstQuestion, GameParticipant firstCaptain) {
        requireCanStart(firstCaptain);
        status = GameStatus.IN_PROGRESS;
        Round firstRound = Round.start(firstCaptain, firstQuestion);
        allRounds.add(firstRound);
        currentRoundNumber = 1;
    }

    public void requireCanStart(GameParticipant firstCaptain) {
        if(!hasEnoughPlayers()) {
            throw new NotEnoughPlayersException(participants.size(), REQUIRED_NUMBER_OF_PARTICIPANTS);
        }
        if(status != GameStatus.WAITING) {
            throw new GameCannotBeStartedException();
        }
        if (!participants.contains(firstCaptain)) {
            throw new InvalidPlayerException();
        }
    }

    public Round startNextRound(Question nextQuestion) {
        if(questionHasBeenUsedBefore(nextQuestion)) {
            throw new CannotUseSameQuestionAgainException();
        }
        final GameParticipant newCaptain = getNextCaptain();
        Round newRound = Round.start(newCaptain, nextQuestion);
        allRounds.add(newRound);
        currentRoundNumber = allRounds.size();
        return newRound;
    }

    private boolean questionHasBeenUsedBefore(Question nextQuestion) {
        return allRounds.stream().map(Round::getQuestion).map(Question::questionId)
                .anyMatch(questionId -> Objects.equals(questionId, nextQuestion.questionId()));
    }

    private GameParticipant getNextCaptain() {
        if (participants.isEmpty()) {
            throw new NoPlayerInGameException();
        }
        if (allRounds.isEmpty()) {
            return participants.getFirst();
        }
        Round lastRound = allRounds.getLast();
        return deriveNextCaptainFromPreviousRound(lastRound);
    }

    private GameParticipant deriveNextCaptainFromPreviousRound(Round lastRound) {
        List<PlayerId> participantIds = participants.stream().map(GameParticipant::playerId).toList();
        PlayerId lastCaptainId = lastRound.getCaptain().playerId();
        int lastCaptainIndex = participantIds.indexOf(lastCaptainId);
        if (lastCaptainIndex == -1) {
            throw new CaptainNotFoundException();
        }
        int indexOfNewCaptain = (lastCaptainIndex + 1) % participants.size();
        return participants.get(indexOfNewCaptain);
    }

    public GameParticipant currentCaptain() {
        return getCurrentRound().getCaptain();
    }
}
