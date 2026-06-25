package com.example.rankinggame.engine.exceptions;

public class CaptainNotFoundException extends RuntimeException {
    public CaptainNotFoundException() {
        super("Captain was not found in this game");
    }
}
