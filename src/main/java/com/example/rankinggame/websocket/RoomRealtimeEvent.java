package com.example.rankinggame.websocket;

public record RoomRealtimeEvent(
        String type,
        Object payload
) {
}
