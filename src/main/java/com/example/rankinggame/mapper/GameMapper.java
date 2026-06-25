package com.example.rankinggame.mapper;

import com.example.rankinggame.engine.Game;
import com.example.rankinggame.engine.GameId;
import com.example.rankinggame.engine.GameStatus;
import com.example.rankinggame.entities.GameSession;
import com.example.rankinggame.entities.GameSessionStatus;
import com.example.rankinggame.entities.GameType;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GameMapper {

    public GameSession toEntity(Game game) {
        GameSession gameSession = new GameSession();
        var gameId = new GameId(UUID.randomUUID());
        gameSession.setId(gameId.value());
        // gameSession.setRounds(rounds);
        gameSession.setStatus(toEntityStatus(game.getStatus()));
        gameSession.setCurrentRoundNumber(game.getCurrentRoundIndex() + 1);
        gameSession.setGameType(GameType.RANKING_GAME);
        return gameSession;
    }

    private GameSessionStatus toEntityStatus(GameStatus status) {
        return status == null ? null : GameSessionStatus.valueOf(status.name());
    }
}
