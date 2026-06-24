package com.example.rankinggame.engine;

public class NotEnoughPlayersException extends IllegalArgumentException {
    public NotEnoughPlayersException(int actualParticipants, int requiredParticipants) {
        super("At least " + requiredParticipants + " players are required to start the game");
    }
}
