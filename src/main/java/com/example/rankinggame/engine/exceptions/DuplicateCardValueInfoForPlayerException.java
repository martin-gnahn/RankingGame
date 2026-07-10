package com.example.rankinggame.engine.exceptions;

import com.example.rankinggame.engine.PlayerId;

public class DuplicateCardValueInfoForPlayerException extends IllegalArgumentException {
    public DuplicateCardValueInfoForPlayerException(PlayerId playerId) {
        super("Card value info for player '%s' must be unique".formatted(playerId.value()));
    }
}
