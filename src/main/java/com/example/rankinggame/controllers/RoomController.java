package com.example.rankinggame.controllers;

import com.example.rankinggame.dto.CreateRoomRequest;
import com.example.rankinggame.dto.ErrorResponse;
import com.example.rankinggame.dto.JoinRoomRequest;
import com.example.rankinggame.dto.RoomActionResponse;
import com.example.rankinggame.dto.RoomPlayerResponse;
import com.example.rankinggame.dto.RoomResponse;
import com.example.rankinggame.usecases.CreateRoomCommand;
import com.example.rankinggame.usecases.CreateRoomResult;
import com.example.rankinggame.usecases.CreateRoomService;
import com.example.rankinggame.usecases.GetRoomService;
import com.example.rankinggame.usecases.JoinRoomCommand;
import com.example.rankinggame.usecases.JoinRoomResult;
import com.example.rankinggame.usecases.JoinRoomService;
import com.example.rankinggame.usecases.PlayerDetailsResult;
import com.example.rankinggame.usecases.RoomDetailsResult;
import com.example.rankinggame.usecases.RoomNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    private final CreateRoomService createRoomService;
    private final JoinRoomService joinRoomService;
    private final GetRoomService getRoomService;

    public RoomController(
            CreateRoomService createRoomService,
            JoinRoomService joinRoomService,
            GetRoomService getRoomService
    ) {
        this.createRoomService = createRoomService;
        this.joinRoomService = joinRoomService;
        this.getRoomService = getRoomService;
    }

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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null ? "Invalid request" : error.getDefaultMessage())
                .orElse("Invalid request");
        return ResponseEntity.badRequest().body(new ErrorResponse(message));
    }

    @ExceptionHandler(RoomNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRoomNotFound(RoomNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(exception.getMessage()));
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
