package com.example.rankinggame.events;

import java.util.UUID;

public record SortingStartedEvent(
        String roomCode,
        UUID roundId
) {
}
