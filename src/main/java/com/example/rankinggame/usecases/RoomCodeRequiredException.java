package com.example.rankinggame.usecases;

public class RoomCodeRequiredException extends RuntimeException {
    public RoomCodeRequiredException() {
        super("Room code is required");
    }
}
