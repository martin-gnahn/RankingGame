package com.example.rankinggame.exceptions;

import com.example.rankinggame.controllers.InvalidTokenException;
import com.example.rankinggame.controllers.errors.ErrorConstants;

public class UserTokenNotPresentException extends InvalidTokenException {
    public UserTokenNotPresentException() {
        super(ErrorConstants.NO_TOKEN, "User token is not present.");
    }
}
