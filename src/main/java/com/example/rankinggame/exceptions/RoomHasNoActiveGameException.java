package com.example.rankinggame.exceptions;

public class RoomHasNoActiveGameException extends RuntimeException {
    public RoomHasNoActiveGameException(String roomCode) {
        super("Room '%s' has no active game.".formatted(roomCode));
    }
}
