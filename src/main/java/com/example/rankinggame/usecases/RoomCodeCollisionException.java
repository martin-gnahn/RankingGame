package com.example.rankinggame.usecases;

public class RoomCodeCollisionException extends RuntimeException {
    RoomCodeCollisionException(Throwable cause) {
        super("Generated room code already exists", cause);
    }
}
