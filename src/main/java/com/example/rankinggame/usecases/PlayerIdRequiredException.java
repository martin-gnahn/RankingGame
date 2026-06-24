package com.example.rankinggame.usecases;

public class PlayerIdRequiredException extends RuntimeException {
    public PlayerIdRequiredException() {
        super("Player id is required");
    }
}
