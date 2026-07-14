package com.example.rankinggame.controllers;

import lombok.Getter;

@Getter
public class UserSessionExpiredException extends InvalidTokenException {
    public UserSessionExpiredException() {
        super(ErrorConstants.TOKEN_EXPIRED, "User session is expired.");
    }
}
