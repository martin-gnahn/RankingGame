package com.example.rankinggame.exceptions;

public class RoomNotInLobbyException extends RuntimeException {
    public RoomNotInLobbyException(String roomCode) {
        super("Room '%s' is not in lobby.".formatted(roomCode));
    }
}
