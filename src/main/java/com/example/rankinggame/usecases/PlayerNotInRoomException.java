package com.example.rankinggame.usecases;

public class PlayerNotInRoomException extends RuntimeException {
    public PlayerNotInRoomException() {
        super("Player is not part of this room");
    }
}
