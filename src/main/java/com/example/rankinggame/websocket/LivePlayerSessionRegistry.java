package com.example.rankinggame.websocket;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class LivePlayerSessionRegistry {
    private final ConcurrentMap<String, LivePlayerSession> sessions = new ConcurrentHashMap<>();

    public void register(String sessionId, String roomCode, UUID playerId) {
        if (sessionId == null || sessionId.isBlank() || roomCode == null || playerId == null) {
            return;
        }

        sessions.put(sessionId, new LivePlayerSession(
                roomCode.trim().toUpperCase(Locale.ROOT),
                playerId
        ));
    }

    public Optional<LivePlayerSession> remove(String sessionId) {
        if (sessionId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(sessions.remove(sessionId));
    }
}
