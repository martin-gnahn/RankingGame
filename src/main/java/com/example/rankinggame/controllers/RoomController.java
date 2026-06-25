package com.example.rankinggame.controllers;

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
import com.example.rankinggame.usecases.CreateRoomService;
import com.example.rankinggame.usecases.GetRoomService;
import com.example.rankinggame.usecases.JoinRoomService;
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
