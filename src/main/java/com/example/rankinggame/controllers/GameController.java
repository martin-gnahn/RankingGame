package com.example.rankinggame.controllers;

import com.example.rankinggame.dto.*;
import com.example.rankinggame.entities.GameSessionPlayerEntity;
import com.example.rankinggame.usecases.GetActiveRoundService;
import com.example.rankinggame.usecases.StartRankingGameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/rooms/{roomCode}/ranking-game")
public class GameController {
    private final ObjectMapper objectMapper;
    private final StartRankingGameService startRankingGameService;
    private final GetActiveRoundService getActiveRoundService;

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    public StartGameResponse startRankingGame(
            @PathVariable String roomCode,
            @Valid @RequestBody StartGameRequest request
    ) {
        StartRankingGameResult result = startRankingGameService.startGame(new StartRankingGameCommand(
                roomCode,
                request == null ? null : request.hostPlayerId()
        ));
        return new StartGameResponse(
                result.room().id(),
                result.room().code(),
                result.game().id(),
                result.game().gameType().name(),
                result.round().id(),
                result.round().index(),
                result.round().questionId()
        );
    }

    @GetMapping("/current-round")
    public ActiveRoundResponse getActiveRound(
            @PathVariable String roomCode,
            @RequestParam UUID playerId
    ) {
        // TODO: rework that. dirty.
        ActiveRoundResult result = getActiveRoundService.loadActiveRoundForPlayer(roomCode, playerId);
        log.info("ActiveRoundResult {}", objectMapper.writeValueAsString(result));
        return new ActiveRoundResponse(
                result.roomId(),
                result.roomCode(),
                result.gameSessionId(),
                result.roundId(),
                result.roundIndex(),
                result.questionId(),
                result.questionText(),
                result.assignedCardValue(),
                result.currentPlayerSubmitted()
        );
    }

    @GetMapping("/current-round/players")
    public List<GameSessionPlayerEntity> getActivePlayers(
            @PathVariable String roomCode
    ) {
        return startRankingGameService.getActivePlayers(roomCode);
    }
}
