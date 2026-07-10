package com.example.rankinggame.engine.exceptions;

import com.example.rankinggame.engine.PlayerId;

public class MissingCardValueForRankedAnswerException extends IllegalArgumentException {
    public MissingCardValueForRankedAnswerException(PlayerId playerId) {
        super("Missing card value for ranked answer player '%s'".formatted(playerId.value()));
    }
}
