package com.example.rankinggame.mapper;

import com.example.rankinggame.engine.Game;
import com.example.rankinggame.engine.GameId;
import com.example.rankinggame.engine.GameParticipant;
import com.example.rankinggame.engine.Round;
import com.example.rankinggame.entities.GameSession;
import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.entities.RoundEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@Service
public class GameMapper {
    private final PlayerMapper playerMapper;
    private final RoundMapper roundMapper;

//    public Game toDomain(GameSession gameSession, List<PlayerEntity> playerEntities) {
//        List<GameParticipant> participants = nullToEmpty(playerEntities).stream()
//                .map(playerMapper::toDomain)
//                .toList();
//        List<Round> rounds = nullToEmpty(gameSession.getRounds()).stream()
//                .map(roundMapper::toDomain)
//                .toList();
//        return Game.builder()
//                .gameId(new GameId(gameSession.getId()))
//                .participants(participants)
//                .status(gameSession.getStatus())
//                .allRounds(rounds)
//                .currentRoundNumber(gameSession.getCurrentRoundNumber())
//                .build();
//    }

    public GameSession toEntity(Game game) {
        List<PlayerEntity> players = nullToEmpty(game.getParticipants()).stream()
                .map(playerMapper::toEntity)
                .toList();
        List<RoundEntity> rounds = nullToEmpty(game.getAllRounds()).stream()
                .map(roundMapper::toEntity)
                .toList();

        GameSession gameSession = new GameSession();
        gameSession.setId(game.getGameId() == null ? null : game.getGameId().value());
        // gameSession.setPlayers(players);
        gameSession.setRounds(rounds);
        gameSession.setStatus(game.getStatus());
        gameSession.setCurrentRoundNumber(game.getCurrentRoundNumber());
        return gameSession;
    }

    private <T> List<T> nullToEmpty(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }
}
