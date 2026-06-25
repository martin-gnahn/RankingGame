package com.example.rankinggame.usecases;

public class PlayerNameTooLongException extends IllegalArgumentException {
    public PlayerNameTooLongException(int maxLength) {
        super("Player name must be " + maxLength + " characters or fewer");
    }
}
