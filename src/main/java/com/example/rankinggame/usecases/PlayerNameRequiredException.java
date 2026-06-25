package com.example.rankinggame.usecases;

public class PlayerNameRequiredException extends IllegalArgumentException {
    public PlayerNameRequiredException() {
        super("Player name is required");
    }
}
