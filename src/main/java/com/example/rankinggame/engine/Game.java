package com.example.rankinggame.engine;

import com.example.rankinggame.engine.exceptions.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private GamePoints gamePoints;

    public Game(List<GameParticipant> participants) {
        // this.gameId = new GameId(UUID.randomUUID());
        this.participants = participants;
        this.allRounds = new ArrayList<>();
        this.status = GameStatus.WAITING;
        this.gamePoints = GamePoints.inactive();
    }

    /**
     * this is a 0-based round index
     */
    private int currentRoundIndex;

    public Round getCurrentRound() {
        return allRounds.get(currentRoundIndex);
    }

    public Optional<Integer> getScore() {
        return Optional.ofNullable(gamePoints)
                .flatMap(GamePoints::value);
    }

    public boolean hasEnoughPlayers() {
        return participants.size() >= REQUIRED_NUMBER_OF_PARTICIPANTS;
    }

    public void start(GameParticipant firstCaptain, Question firstQuestion) {
        requireCanStart(firstCaptain);
        status = GameStatus.IN_PROGRESS;
        Round firstRound = Round.start(firstCaptain, firstQuestion);
        allRounds.add(firstRound);
        currentRoundIndex = 0;
        gamePoints = GamePoints.starting(GameConstants.DEFAULT_STARTING_POINTS);
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
        currentRoundIndex = allRounds.size() - 1;
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
