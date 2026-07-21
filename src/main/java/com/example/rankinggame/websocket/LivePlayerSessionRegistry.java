package com.example.rankinggame.websocket;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class LivePlayerSessionRegistry {
    private final ConcurrentMap<String, LivePlayerSession> sessions = new ConcurrentHashMap<>();

    public Optional<LivePlayerSession> register(String sessionId, String roomCode, UUID playerId) {
        if (sessionId == null || sessionId.isBlank() || roomCode == null || playerId == null) {
            return Optional.empty();
        }

        LivePlayerSession session = new LivePlayerSession(normalizeRoomCode(roomCode), playerId);
        sessions.put(sessionId, session);
        return Optional.of(session);
    }

    public boolean hasSession(String roomCode, UUID playerId) {
        if (roomCode == null || playerId == null) {
            return false;
        }

        String normalizedRoomCode = normalizeRoomCode(roomCode);
        return sessions.values().stream()
                .anyMatch(session -> Objects.equals(session.playerId(), playerId)
                        && Objects.equals(session.roomCode(), normalizedRoomCode));
    }

    public Optional<LivePlayerSession> remove(String sessionId) {
        if (sessionId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(sessions.remove(sessionId));
    }

    private String normalizeRoomCode(String roomCode) {
        return roomCode.trim().toUpperCase(Locale.ROOT);
    }
}
