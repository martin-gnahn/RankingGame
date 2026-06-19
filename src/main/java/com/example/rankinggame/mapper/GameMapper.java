package com.example.rankinggame.mapper;

import com.example.rankinggame.engine.Game;
import com.example.rankinggame.engine.GameId;
import com.example.rankinggame.engine.Player;
import com.example.rankinggame.engine.Round;
import com.example.rankinggame.entities.GameSession;
import com.example.rankinggame.entities.GameSessionStatus;
import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.entities.RoundEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class GameMapper {
    private final PlayerMapper playerMapper;
    private final RoundMapper roundMapper;

    public Game toDomain(GameSession gameSession) {
        List<Player> players = gameSession.getPlayers().stream()
                .map(playerMapper::toDomain)
                .toList();
        List<Round> rounds = gameSession.getRounds().stream()
                .map(roundMapper::toDomain)
                .toList();
        return Game.builder()
                .gameId(new GameId(gameSession.getId()))
                .players(players)
                .status(gameSession.getStatus())
                .allRounds(rounds)
                .currentRoundNumber(gameSession.getCurrentRoundNumber())
                .build();
    }
}
