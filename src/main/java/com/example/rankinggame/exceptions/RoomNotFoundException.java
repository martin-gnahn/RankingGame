package com.example.rankinggame.exceptions;

public class RoomNotFoundException extends RuntimeException {
    public RoomNotFoundException(String roomCode) {
        super("Room not found: " + roomCode);
    }
}
