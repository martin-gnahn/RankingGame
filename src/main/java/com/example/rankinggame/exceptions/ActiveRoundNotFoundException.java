package com.example.rankinggame.exceptions;

public class ActiveRoundNotFoundException extends RuntimeException {
    public ActiveRoundNotFoundException(String roomCode) {
        super("Room '%s' has no active round.".formatted(roomCode));
    }
}
