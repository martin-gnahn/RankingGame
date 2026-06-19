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
    GameId gameId;
    private List<Player> players;
    private List<GameParticipant> participants;
    boolean isActive;
    private List<Round> allRounds = new ArrayList<>();
    private GameSessionStatus status;

    private int currentRoundNumber;

    public Round getCurrentRound() {
        return allRounds.get(currentRoundNumber);
    }

    public Game(List<Player> players) {
        this.players = players;
        this.gameId = new GameId(UUID.randomUUID());
        this.isActive = true;
    }

    public boolean canStart() {
       return players.size() >= 3 && status == GameSessionStatus.WAITING;
    }

    public boolean isActive() {
        return status == GameSessionStatus.IN_PROGRESS;
    }

    public void start() {
        if(!canStart()) {
            throw new GameCannotBeStartedException();
        }
        status = GameSessionStatus.IN_PROGRESS;
        Player firstCaptain = players.getFirst();
        Round firstRound = Round.start(firstCaptain.playerId());
        allRounds.add(firstRound);
    }

    public Round startNextRound(QuestionId questionId) {
        final GameParticipant newCaptain = getNextCaptain();
//        Player lastCaptain = players.stream()
//                .filter(p -> p.playerId() == lastRound.getCaptainPlayerId())
//                .findFirst().orElseThrow(CaptainNotFoundException::new);
        // TODO: optimize later
        Round newRound = Round.start(newCaptain.playerId());
        allRounds.add(newRound);
        return newRound;
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
        PlayerId lastCaptainId = lastRound.getCaptainPlayerId();
        int lastCaptainIndex = participantIds.indexOf(lastCaptainId);
        if (lastCaptainIndex == -1) {
            throw new CaptainNotFoundException();
        }
        int indexOfNewCaptain = (lastCaptainIndex + 1) % participants.size();
        return participants.get(indexOfNewCaptain);
    }

    public PlayerId currentCaptainId() {
        return getCurrentRound().getCaptainPlayerId();
    }
}
