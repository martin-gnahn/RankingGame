package com.example.rankinggame.controllers;

import com.example.rankinggame.dto.*;
import com.example.rankinggame.engine.GameConstants;
import com.example.rankinggame.usecases.RankAnswerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/rooms/{roomCode}/ranking-game/rounds/{roundId}/answer/position")
public class RankAnswerController {
    private final RankAnswerService rankAnswerService;
    private final PlayerSessionService playerSessionService;

    @PostMapping("/new")
    public HttpEntity<RankAnswerResponse> addRankingPosition(
            @PathVariable String roomCode,
            @PathVariable UUID roundId,
            @Valid @RequestBody AddRankingPositionRequest request,
            @RequestHeader(GameConstants.PLAYER_SESSION_TOKEN) String token
    ) {
        AuthenticatedPlayer player =
                playerSessionService.authenticatePlayer(roomCode, token);
        RankAnswerResult rankAnswerResult = rankAnswerService.addRankingPosition(new AddRankingPositionCommand(
                roomCode,
                roundId,
                player.playerId(),
                request.answerId()
        ));
        return ResponseEntity.ok(RankAnswerResponse.from(rankAnswerResult));
    }

    @GetMapping("/all")
    public RankedAnswerListResponse getOrderOfAnswers(
            @PathVariable String roomCode,
            @PathVariable UUID roundId,
            @RequestHeader(GameConstants.PLAYER_SESSION_TOKEN) String token
    ) {
        AuthenticatedPlayer player =
                playerSessionService.authenticatePlayer(roomCode, token);
        List<RankedAnswerDto> rankingPositions = rankAnswerService.getRankingPositions(new GetRankingPositionsCommand(
                roomCode,
                roundId,
                player.playerId()
        ));
        return new RankedAnswerListResponse(rankingPositions);
    }


}
