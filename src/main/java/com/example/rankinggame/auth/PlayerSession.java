package com.example.rankinggame.auth;

import java.time.Instant;
import java.util.UUID;

public record PlayerSession(Instant sessionExpiresAt, UUID playerId) {
}
