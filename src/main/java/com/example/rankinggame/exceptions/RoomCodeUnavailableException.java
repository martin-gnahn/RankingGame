package com.example.rankinggame.exceptions;

public class RoomCodeUnavailableException extends RuntimeException {
    public RoomCodeUnavailableException(Throwable cause) {
        super("Unable to allocate a unique room code", cause);
    }
}
