package com.example.rankinggame.usecases;

public class RoomNotFoundException extends RuntimeException {
    public RoomNotFoundException(String roomCode) {
        super("Room not found: " + roomCode);
    }
}
