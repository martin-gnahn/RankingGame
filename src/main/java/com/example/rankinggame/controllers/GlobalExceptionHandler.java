package com.example.rankinggame.controllers;

import com.example.rankinggame.dto.ApiError;
import com.example.rankinggame.engine.exceptions.AnswerAlreadySubmittedException;
import com.example.rankinggame.engine.exceptions.AnswerTextRequiredException;
import com.example.rankinggame.engine.exceptions.AnswerTextTooLongException;
import com.example.rankinggame.engine.exceptions.AnswersNotAcceptedException;
import com.example.rankinggame.engine.exceptions.CannotUseSameQuestionAgainException;
import com.example.rankinggame.engine.exceptions.CaptainNotFoundException;
import com.example.rankinggame.engine.exceptions.GameCannotBeStartedException;
import com.example.rankinggame.engine.exceptions.InvalidCardValueException;
import com.example.rankinggame.engine.exceptions.InvalidPlayerException;
import com.example.rankinggame.engine.exceptions.NoPlayerInGameException;
import com.example.rankinggame.engine.exceptions.NotEnoughPlayersException;
import com.example.rankinggame.exceptions.QuestionUnavailableException;
import com.example.rankinggame.exceptions.RoomCodeUnavailableException;
import com.example.rankinggame.exceptions.RoomNotFoundException;
import com.example.rankinggame.usecases.HostPlayerIdRequiredException;
import com.example.rankinggame.usecases.OnlyHostCanStartGame;
import com.example.rankinggame.usecases.PlayerIdRequiredException;
import com.example.rankinggame.usecases.PlayerNameAlreadyTakenException;
import com.example.rankinggame.usecases.PlayerNameRequiredException;
import com.example.rankinggame.usecases.PlayerNameTooLongException;
import com.example.rankinggame.usecases.RoomCodeRequiredException;
import com.example.rankinggame.usecases.RoundIdRequiredException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(error(ErrorConstants.INVALID_REQUEST, exception.getMessage()));
    }

    @ExceptionHandler({
            RoomCodeRequiredException.class,
            RoundIdRequiredException.class,
            PlayerIdRequiredException.class,
            HostPlayerIdRequiredException.class,
            PlayerNameRequiredException.class,
            PlayerNameTooLongException.class,
            AnswerTextRequiredException.class,
            AnswerTextTooLongException.class,
            InvalidCardValueException.class,
            InvalidPlayerException.class
    })
    public ResponseEntity<ApiError> handleRequiredRequestValue(RuntimeException exception) {
        return ResponseEntity.badRequest().body(error(ErrorConstants.INVALID_REQUEST, exception.getMessage()));
    }

    @ExceptionHandler(OnlyHostCanStartGame.class)
    public ResponseEntity<ApiError> handleAccessDenied(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error(ErrorConstants.ACCESS_DENIED, exception.getMessage()));
    }

    @ExceptionHandler(PlayerNameAlreadyTakenException.class)
    public ResponseEntity<ApiError> handlePlayerNameAlreadyTaken(PlayerNameAlreadyTakenException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(ErrorConstants.PLAYER_NAME_ALREADY_TAKEN, exception.getMessage()));
    }

    @ExceptionHandler(AnswerAlreadySubmittedException.class)
    public ResponseEntity<ApiError> handleAnswerAlreadySubmitted(AnswerAlreadySubmittedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(ErrorConstants.ANSWER_ALREADY_SUBMITTED, exception.getMessage()));
    }

    @ExceptionHandler({
            AnswersNotAcceptedException.class,
            CannotUseSameQuestionAgainException.class,
            CaptainNotFoundException.class,
            GameCannotBeStartedException.class,
            NoPlayerInGameException.class,
            NotEnoughPlayersException.class
    })
    public ResponseEntity<ApiError> handleGameStateConflict(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(ErrorConstants.GAME_STATE_CONFLICT, exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst().filter(error -> error.getDefaultMessage() != null)
                .map(DefaultMessageSourceResolvable::getDefaultMessage).orElse(ErrorConstants.INVALID_REQUEST);
        return ResponseEntity.badRequest().body(error(ErrorConstants.VALIDATION_ERROR, message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableMessage(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(error(ErrorConstants.INVALID_REQUEST_BODY, "Invalid request body"));
    }

    @ExceptionHandler(RoomNotFoundException.class)
    public ResponseEntity<ApiError> handleRoomNotFound(RoomNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ErrorConstants.ROOM_NOT_FOUND, exception.getMessage()));
    }

    @ExceptionHandler(RoomCodeUnavailableException.class)
    public ResponseEntity<ApiError> handleRoomCodeUnavailable(RoomCodeUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(error(ErrorConstants.ROOM_CODE_UNAVAILABLE, exception.getMessage()));
    }

    @ExceptionHandler(QuestionUnavailableException.class)
    public ResponseEntity<ApiError> handleQuestionUnavailable(QuestionUnavailableException exception) {
        log.error("Question unavailable: {}", String.valueOf(exception));
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(error(ErrorConstants.QUESTION_UNAVAILABLE, exception.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(NoResourceFoundException exception) {
        log.error("Not found: {}", String.valueOf(exception));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ErrorConstants.RESOURCE_NOT_FOUND, "Resource not found"));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiError> handleNoHandlerFound(NoHandlerFoundException exception) {
        log.error("Not found: {}", String.valueOf(exception));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ErrorConstants.RESOURCE_NOT_FOUND, "Resource not found"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        log.error("Unknown internal error: {}", String.valueOf(exception));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(ErrorConstants.INTERNAL_ERROR, "An unexpected error occurred"));
    }

    private ApiError error(String code, String message) {
        return new ApiError(code, message);
    }
}
