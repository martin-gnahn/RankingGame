package com.example.rankinggame.engine.exceptions;

import com.example.rankinggame.engine.CardNumber;

public class DuplicateCardValueInfoForCardNumberException extends IllegalArgumentException {
    public DuplicateCardValueInfoForCardNumberException(CardNumber cardNumber) {
        super("Card value info for card number '%d' must be unique".formatted(cardNumber.value()));
    }
}
