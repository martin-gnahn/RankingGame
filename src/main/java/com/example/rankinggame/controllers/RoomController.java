package com.example.rankinggame.controllers;

import com.example.rankinggame.dto.*;
import com.example.rankinggame.usecases.CreateRoomService;
import com.example.rankinggame.usecases.GetRoomService;
import com.example.rankinggame.usecases.JoinRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    private final CreateRoomService createRoomService;
    private final JoinRoomService joinRoomService;
    private final GetRoomService getRoomService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomActionResponse createRoom(@Valid @RequestBody(required = false) CreateRoomRequest request) {
        String playerName = request == null ? null : request.playerName();
        CreateRoomResult result = createRoomService.createRoom(new CreateRoomCommand(playerName));
        return new RoomActionResponse(result.roomCode(), result.roomId(), result.playerId(), result.playerName(), result.playerToken(), true);
    }

    @PostMapping("/{roomCode}/players")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomActionResponse joinRoom(
            @PathVariable String roomCode,
            @Valid @RequestBody(required = false) JoinRoomRequest request
    ) {
        String playerName = request == null ? null : request.playerName();
        JoinRoomResult result = joinRoomService.joinRoom(new JoinRoomCommand(roomCode, playerName));
        return new RoomActionResponse(result.roomCode(), result.roomId(), result.playerId(), result.playerName(), result.playerToken(), false);
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
                        .toList(),
                result.canStartGame(),
                result.startBlockedReason()
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
