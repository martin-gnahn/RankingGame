package com.example.rankinggame.engine.exceptions;

import com.example.rankinggame.engine.PlayerId;

public class DuplicateRankedAnswerForPlayerException extends IllegalArgumentException {
    public DuplicateRankedAnswerForPlayerException(PlayerId playerId) {
        super("Ranked answer for player '%s' must be unique".formatted(playerId.value()));
    }
}
