package com.example.rankinggame.controllers;

import com.example.rankinggame.dto.ActiveRoundResponse;
import com.example.rankinggame.dto.ActiveRoundResult;
import com.example.rankinggame.dto.StartGameRequest;
import com.example.rankinggame.dto.StartGameResponse;
import com.example.rankinggame.dto.StartRankingGameCommand;
import com.example.rankinggame.dto.StartRankingGameResult;
import com.example.rankinggame.usecases.GetActiveRoundService;
import com.example.rankinggame.usecases.StartRankingGameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/rooms/{roomCode}/ranking-game")
public class RankingGameController {
    private final ObjectMapper objectMapper;
    private final StartRankingGameService startRankingGameService;
    private final GetActiveRoundService getActiveRoundService;

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    public StartGameResponse startRankingGame(
            @PathVariable String roomCode,
            @Valid @RequestBody(required = false) StartGameRequest request
    ) {
        StartRankingGameResult result = startRankingGameService.startGame(new StartRankingGameCommand(
                roomCode,
                request == null ? null : request.hostPlayerId()
        ));
        return new StartGameResponse(
                result.roomId(),
                result.roomCode(),
                result.gameSessionId(),
                result.gameType().name(),
                result.roundId(),
                result.roundNumber(),
                result.questionId()
        );
    }

    @GetMapping("/current-round")
    public ActiveRoundResponse getActiveRound(
            @PathVariable String roomCode,
            @RequestParam UUID playerId
    ) {
        ActiveRoundResult result = getActiveRoundService.getActiveRound(roomCode, playerId);
        log.info("ActiveRoundResult {}", objectMapper.writeValueAsString(result));
        return new ActiveRoundResponse(
                result.roomId(),
                result.roomCode(),
                result.gameSessionId(),
                result.roundId(),
                result.roundNumber(),
                result.questionId(),
                result.questionText(),
                result.assignedCardValue()
        );
    }
}
