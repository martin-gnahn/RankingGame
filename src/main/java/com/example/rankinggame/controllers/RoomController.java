package com.example.rankinggame.controllers;

import com.example.rankinggame.dto.ActiveRoundResponse;
import com.example.rankinggame.dto.ActiveRoundResult;
import com.example.rankinggame.dto.CreateRoomCommand;
import com.example.rankinggame.dto.CreateRoomRequest;
import com.example.rankinggame.dto.CreateRoomResult;
import com.example.rankinggame.dto.JoinRoomCommand;
import com.example.rankinggame.dto.JoinRoomRequest;
import com.example.rankinggame.dto.JoinRoomResult;
import com.example.rankinggame.dto.PlayerDetailsResult;
import com.example.rankinggame.dto.RoomActionResponse;
import com.example.rankinggame.dto.RoomDetailsResult;
import com.example.rankinggame.dto.RoomPlayerResponse;
import com.example.rankinggame.dto.RoomResponse;
import com.example.rankinggame.dto.StartRankingGameCommand;
import com.example.rankinggame.dto.StartGameRequest;
import com.example.rankinggame.dto.StartGameResponse;
import com.example.rankinggame.dto.StartRankingGameResult;
import com.example.rankinggame.dto.SubmitAnswerCommand;
import com.example.rankinggame.dto.SubmitAnswerRequest;
import com.example.rankinggame.dto.SubmitAnswerResponse;
import com.example.rankinggame.dto.SubmitAnswerResult;
import com.example.rankinggame.usecases.CreateRoomService;
import com.example.rankinggame.usecases.GetActiveRoundService;
import com.example.rankinggame.usecases.GetRoomService;
import com.example.rankinggame.usecases.JoinRoomService;
import com.example.rankinggame.usecases.StartRankingGameService;
import com.example.rankinggame.usecases.SubmitAnswerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    private final CreateRoomService createRoomService;
    private final JoinRoomService joinRoomService;
    private final GetRoomService getRoomService;
    private final StartRankingGameService startRankingGameService;
    private final GetActiveRoundService getActiveRoundService;
    private final SubmitAnswerService submitAnswerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomActionResponse createRoom(@Valid @RequestBody(required = false) CreateRoomRequest request) {
        String playerName = request == null ? null : request.playerName();
        CreateRoomResult result = createRoomService.createRoom(new CreateRoomCommand(playerName));
        return new RoomActionResponse(result.roomCode(), result.roomId(), result.playerId(), result.playerName(), true);
    }

    @PostMapping("/{roomCode}/players")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomActionResponse joinRoom(
            @PathVariable String roomCode,
            @Valid @RequestBody(required = false) JoinRoomRequest request
    ) {
        String playerName = request == null ? null : request.playerName();
        JoinRoomResult result = joinRoomService.joinRoom(new JoinRoomCommand(roomCode, playerName));
        return new RoomActionResponse(result.roomCode(), result.roomId(), result.playerId(), result.playerName(), false);
    }

    @GetMapping("/{roomCode}")
    public RoomResponse getRoom(@PathVariable String roomCode) {
        RoomDetailsResult result = getRoomService.getRoom(roomCode);
        return new RoomResponse(
                result.roomId(),
                result.roomCode(),
                result.status().name(),
                result.players().stream()
                        .map(this::toPlayerResponse)
                        .toList()
        );
    }

    @PostMapping("/{roomCode}/ranking-game/start")
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

    @GetMapping("/{roomCode}/ranking-game/current-round")
    public ActiveRoundResponse getActiveRound(@PathVariable String roomCode) {
        ActiveRoundResult result = getActiveRoundService.getActiveRound(roomCode);
        return new ActiveRoundResponse(
                result.roomId(),
                result.roomCode(),
                result.gameSessionId(),
                result.roundId(),
                result.roundNumber(),
                result.questionId(),
                result.questionText()
        );
    }

    @PostMapping("/{roomCode}/ranking-game/rounds/{roundId}/answers")
    @ResponseStatus(HttpStatus.CREATED)
    public SubmitAnswerResponse submitAnswer(
            @PathVariable String roomCode,
            @PathVariable UUID roundId,
            @Valid @RequestBody(required = false) SubmitAnswerRequest request
    ) {
        SubmitAnswerResult result = submitAnswerService.submitAnswer(new SubmitAnswerCommand(
                roomCode,
                roundId,
                request == null ? null : request.playerId(),
                request == null ? null : request.answerText(),
                request == null ? 0 : request.cardValue()
        ));

        return new SubmitAnswerResponse(
                result.answerId(),
                result.roundId(),
                result.playerId(),
                result.submitted()
        );
    }

    private RoomPlayerResponse toPlayerResponse(PlayerDetailsResult player) {
        return new RoomPlayerResponse(
                player.playerId(),
                player.nickname(),
                player.host(),
                player.connectionStatus().name()
        );
    }
}
