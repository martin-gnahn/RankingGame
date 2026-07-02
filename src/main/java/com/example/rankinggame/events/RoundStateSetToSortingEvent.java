
package com.example.rankinggame.events;

import java.util.UUID;

public record RoundStateSetToSortingEvent(
        String roomCode,
        UUID roundId
) {
}
