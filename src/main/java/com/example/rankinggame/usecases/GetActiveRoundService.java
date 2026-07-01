package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.ActiveRoundResult;
import com.example.rankinggame.entities.*;
import com.example.rankinggame.exceptions.ActiveRoundNotFoundException;
import com.example.rankinggame.exceptions.ActiveRoundQuestionNotFoundException;
import com.example.rankinggame.exceptions.RoomHasNoActiveGameException;
import com.example.rankinggame.exceptions.RoomNotFoundException;
import com.example.rankinggame.mapper.GameMapper;
import com.example.rankinggame.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetActiveRoundService {
    private final RoomRepository roomRepository;
    private final GameSessionRepository gameSessionRepository;
    private final RoundRepository roundRepository;
    private final QuestionRepository questionRepository;
    private final RoundCardAssignmentService roundCardAssignmentService;
    private final JpaPlayerRepository playerRepository;
    private final GameMapper gameMapper;

    @Transactional
    public ActiveRoundResult getActiveRound(String roomCode, java.util.UUID playerId) {
        String normalizedRoomCode = normalizeRoomCode(roomCode);
        RoomEntity room = roomRepository.findByCode(normalizedRoomCode)
                .orElseThrow(() -> new RoomNotFoundException(normalizedRoomCode));
        log.info("Retrieved Room entity with code '{}' and id '{}'", room.getCode(), room.getId());

        if (room.getStatus() != RoomStatus.IN_GAME) {
            throw new RoomHasNoActiveGameException(normalizedRoomCode);
        }

        GameSession gameSession = gameSessionRepository.findByRoomId(room.getId())
                .orElseThrow(() -> new RoomHasNoActiveGameException(normalizedRoomCode));
        log.info("Retrieved GameSession entity with id '{}'", gameSession.getId());

        RoundEntity round = roundRepository.findById(gameSession.getCurrentRoundId())
                .orElseThrow(() -> new ActiveRoundNotFoundException(normalizedRoomCode));
        log.info("Retrieved Round entity with id '{}'", round.getId());
        QuestionEntity question = questionRepository.findById(round.getQuestionId())
                .orElseThrow(() -> new ActiveRoundQuestionNotFoundException(round.getId(), round.getQuestionId()));
        log.info("Retrieved Question entity with id '{}'", question.getId());
        int assignedCardValue = roundCardAssignmentService.getCardValue(room.getId(), round.getId(), playerId);

        log.info("Retrieved active round result for room '{}'", room.getId());
        int roundNumber = gameSession.getCurrentRoundIndex() + 1;
        return new ActiveRoundResult(
                room.getId(),
                room.getCode(),
                gameSession.getId(),
                round.getId(),
                roundNumber,
                question.getId(),
                question.getText(),
                assignedCardValue
        );
    }

    private String normalizeRoomCode(String roomCode) {
        if (roomCode == null || roomCode.isBlank()) {
            throw new RoomNotFoundException("");
        }

        return roomCode.trim().toUpperCase(Locale.ROOT);
    }
}
