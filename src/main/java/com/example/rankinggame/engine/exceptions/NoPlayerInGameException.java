package com.example.rankinggame.engine.exceptions;

public class NoPlayerInGameException extends RuntimeException {
    public NoPlayerInGameException() {
        super("No players are in this game");
    }
}
