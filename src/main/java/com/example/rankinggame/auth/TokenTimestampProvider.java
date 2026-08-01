package com.example.rankinggame.auth;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TokenTimestampProvider {
    public Instant getTokenExpirationDate() {
        return Instant.now().plus(1, ChronoUnit.HOURS);
    }
}
