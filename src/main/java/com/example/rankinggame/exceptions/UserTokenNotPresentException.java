package com.example.rankinggame.exceptions;

import com.example.rankinggame.controllers.ErrorConstants;
import com.example.rankinggame.controllers.InvalidTokenException;

public class UserTokenNotPresentException extends InvalidTokenException {
    public UserTokenNotPresentException() {
        super(ErrorConstants.NO_TOKEN, "User token is not present.");
    }
}
