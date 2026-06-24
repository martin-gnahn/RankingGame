package com.example.rankinggame.usecases;

public class HostPlayerIdRequiredException extends RuntimeException {
    public HostPlayerIdRequiredException() {
        super("Host player id is required");
    }
}
