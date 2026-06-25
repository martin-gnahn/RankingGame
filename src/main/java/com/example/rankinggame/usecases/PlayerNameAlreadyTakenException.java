package com.example.rankinggame.usecases;

public class PlayerNameAlreadyTakenException extends IllegalArgumentException {
    public PlayerNameAlreadyTakenException() {
        super("Player name is already taken");
    }

    public PlayerNameAlreadyTakenException(Throwable cause) {
        super("Player name is already taken", cause);
    }
}
