package com.example.rankinggame.engine;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public record Player(PlayerId playerId, String playerName) {
}
