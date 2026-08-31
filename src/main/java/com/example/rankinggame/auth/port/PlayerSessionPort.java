package com.example.rankinggame.auth.port;

import com.example.rankinggame.auth.PlayerSession;

import java.util.Optional;

public interface PlayerSessionPort {
    Optional<PlayerSession> findByRoomCodeAndTokenHash(
            String roomCode,
            String tokenHash
    );
}
