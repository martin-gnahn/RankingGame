package com.example.rankinggame.engine.exceptions;

public class InvalidPlayerException extends IllegalArgumentException {
    public InvalidPlayerException() {
        super("Player is not part of this game");
    }
}
