package com.example.rankinggame.engine;

import lombok.Builder;

@Builder
public record Player(PlayerId playerId, String playerName) {
}
