package com.example.rankinggame.controllers;

public class UserNotAuthorizedException extends InvalidTokenException {
    public UserNotAuthorizedException() {
        super(ErrorConstants.TOKEN_NOT_AUTHORIZED, "User is not authorized to access backend.");
    }
}
