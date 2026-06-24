package com.example.rankinggame.engine;

public class NotEnoughPlayersException extends RuntimeException {
    public NotEnoughPlayersException(int actualParticipants, int requiredParticipants) {
        super("Game has not enough participants to be started. Actual: " + actualParticipants + ". Required: " + requiredParticipants);
    }
}
