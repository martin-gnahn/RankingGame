package com.example.rankinggame.controllers;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public abstract class InvalidTokenException extends RuntimeException {
    private final String errorKey;
    private final String message;
}
