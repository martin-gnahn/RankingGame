package com.example.rankinggame.controllers;

import com.example.rankinggame.dto.*;
import com.example.rankinggame.engine.GameConstants;
import com.example.rankinggame.entities.GameSessionPlayerEntity;
import com.example.rankinggame.usecases.GetActiveRoundService;
import com.example.rankinggame.usecases.StartRankingGameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/rooms/{roomCode}/ranking-game")
public class GameController {
    private final ObjectMapper objectMapper;
    private final StartRankingGameService startRankingGameService;
    private final GetActiveRoundService getActiveRoundService;
    private final PlayerSessionService playerSessionService;

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    public StartGameResponse startRankingGame(
            @PathVariable String roomCode,
            @RequestHeader(value = GameConstants.PLAYER_SESSION_TOKEN, required = false) String token
    ) {
        AuthenticatedPlayer player =
                playerSessionService.authenticatePlayer(roomCode, token);
        StartGameResult result = startRankingGameService.startGame(new StartRankingGameCommand(
                roomCode,
                player.playerId()
        ));

        return StartGameResponse.from(result);
    }

    @GetMapping("/current-round")
    public ActiveRoundResponse getActiveRound(
            @PathVariable String roomCode,
            @RequestHeader(value = GameConstants.PLAYER_SESSION_TOKEN, required = false) String token
    ) {
        AuthenticatedPlayer player =
                playerSessionService.authenticatePlayer(roomCode, token);
        ActiveRoundResult result = getActiveRoundService.loadActiveRoundForPlayer(roomCode, player.playerId());
        log.info("ActiveRoundResult {}", objectMapper.writeValueAsString(result));
        return ActiveRoundResponse.from(result);
    }

    @GetMapping("/current-round/players")
    public List<GameSessionPlayerEntity> getActivePlayers(
            @PathVariable String roomCode,
            @RequestHeader(value = GameConstants.PLAYER_SESSION_TOKEN, required = false) String token
    ) {
        playerSessionService.authenticatePlayer(roomCode, token);
        return startRankingGameService.getActivePlayers(roomCode);
    }
}
