package com.example.rankinggame.dto;

public record StartGameResponse(
        String roomCode
) {
    public static StartGameResponse from(StartGameResult result) {
        return new StartGameResponse(result.roomCode());
    }
}
