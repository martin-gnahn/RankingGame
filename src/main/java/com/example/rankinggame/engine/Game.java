package com.example.rankinggame.engine;

import com.example.rankinggame.entities.GameSessionStatus;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// TODO: this is a POJO for Game
@Data
@Builder
public class Game {
    GameId gameId;
    private List<Player> players;
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
        Round firstRound = new Round(firstCaptain.playerId());
        allRounds.add(firstRound);
    }

    public void nextRound() {
        Round lastRound = allRounds.getLast();
//        Player lastCaptain = players.stream()
//                .filter(p -> p.playerId() == lastRound.getCaptainPlayerId())
//                .findFirst().orElseThrow(CaptainNotFoundException::new);
        // TODO: optimize later
        Player newCaptain = players.getFirst();
        Round newRound = new Round(newCaptain.playerId());
        allRounds.add(newRound);
    }
}
