package com.example.rankinggame.usecases;

public class RoundNotPartOfActiveGameException extends RuntimeException {
    public RoundNotPartOfActiveGameException() {
        super("Round is not part of the active game");
    }
}
