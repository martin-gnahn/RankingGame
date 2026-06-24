package com.example.rankinggame.engine;

public class GameCannotBeStartedException extends RuntimeException {
    public GameCannotBeStartedException() {
        super("Game cannot be started.");
    }
}
