package com.example.rankinggame.controllers;

import com.example.rankinggame.dto.CreateRoomRequest;
import com.example.rankinggame.dto.ErrorResponse;
import com.example.rankinggame.dto.RoomActionResponse;
import com.example.rankinggame.usecases.CreateRoomCommand;
import com.example.rankinggame.usecases.CreateRoomResult;
import com.example.rankinggame.usecases.CreateRoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rooms")
public class RoomController {
    private final CreateRoomService createRoomService;

    public RoomController(CreateRoomService createRoomService) {
        this.createRoomService = createRoomService;
    }

    @PostMapping
    public RoomActionResponse createRoom(@RequestBody CreateRoomRequest request) {
        String playerName = request == null ? null : request.playerName();
        CreateRoomResult result = createRoomService.createRoom(new CreateRoomCommand(playerName));
        return new RoomActionResponse(result.roomCode(), result.playerId());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(exception.getMessage()));
    }
}
