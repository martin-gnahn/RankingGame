package com.example.rankinggame.engine.exceptions;

public class GameCannotBeStartedException extends RuntimeException {
    public GameCannotBeStartedException() {
        super("Game cannot be started.");
    }
}
