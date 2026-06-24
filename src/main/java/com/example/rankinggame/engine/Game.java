package com.example.rankinggame.engine;

import com.example.rankinggame.entities.GameSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// TODO: this is a POJO for Game
@Data
@Builder
@AllArgsConstructor
public class Game {
    final GameId gameId;
    private List<GameParticipant> participants;
    boolean isActive;
    private List<Round> allRounds = new ArrayList<>();
    private GameSessionStatus status;

    private int currentRoundNumber;

    public Round getCurrentRound() {
        return allRounds.get(currentRoundNumber);
    }

    public Game(List<GameParticipant> participants) {
        this.gameId = new GameId(UUID.randomUUID());
        this.participants = participants;
        this.status = GameSessionStatus.WAITING;
    }

    public boolean hasEnoughParticipants() {
       return participants.size() >= 2 && status == GameSessionStatus.WAITING;
    }

    public boolean isActive() {
        return status == GameSessionStatus.IN_PROGRESS;
    }

    public void start(Question firstQuestion) {
        if(!hasEnoughParticipants()) {
            throw new GameCannotBeStartedException();
        }
        status = GameSessionStatus.IN_PROGRESS;
        GameParticipant firstCaptain = getNextCaptain();
        Round firstRound = Round.start(firstCaptain, firstQuestion);
        allRounds.add(firstRound);
    }

    public Round startNextRound(Question nextQuestion) {
        if(questionHasBeenUsedBefore(nextQuestion)) {
            throw new CannotUseSameQuestionAgainException();
        }
        final GameParticipant newCaptain = getNextCaptain();
        Round newRound = Round.start(newCaptain, nextQuestion);
        allRounds.add(newRound);
        return newRound;
    }

    private boolean questionHasBeenUsedBefore(Question nextQuestion) {
        return allRounds.stream().map(Round::getQuestion).map(Question::questionId)
                .anyMatch(questionId -> questionId.equals(nextQuestion.questionId()));
    }

    private GameParticipant getNextCaptain() {
        if (participants.isEmpty()) {
            throw new NoPlayerInGameException();
        }
        if (allRounds.isEmpty()) {
            return participants.getFirst();
        }
        Round lastRound = allRounds.getLast();
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
