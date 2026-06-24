package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.ActiveRoundResult;
import com.example.rankinggame.entities.GameSession;
import com.example.rankinggame.entities.QuestionEntity;
import com.example.rankinggame.entities.RoomEntity;
import com.example.rankinggame.entities.RoomStatus;
import com.example.rankinggame.entities.RoundEntity;
import com.example.rankinggame.entities.RoundState;
import com.example.rankinggame.exceptions.RoomNotFoundException;
import com.example.rankinggame.repositories.GameSessionRepository;
import com.example.rankinggame.repositories.QuestionRepository;
import com.example.rankinggame.repositories.RoomRepository;
import com.example.rankinggame.repositories.RoundRepository;
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

    @Transactional
    public ActiveRoundResult getActiveRound(String roomCode, java.util.UUID playerId) {
        String normalizedRoomCode = normalizeRoomCode(roomCode);
        RoomEntity room = roomRepository.findByCode(normalizedRoomCode)
                .orElseThrow(() -> new RoomNotFoundException(normalizedRoomCode));
        log.info("Retrieved Room entity with code '{}' and id '{}'", room.getCode(), room.getId());

        if (room.getStatus() != RoomStatus.IN_GAME) {
            throw new IllegalArgumentException("No active game is running");
        }

        // TODO: Prefer custom exceptions. maybe no hardcoded error message strings at this service.
        GameSession gameSession = gameSessionRepository.findByRoomId(room.getId())
                .orElseThrow(() -> new IllegalArgumentException("No active game is running"));
        log.info("Retrieved GameSession entity with id '{}'", gameSession.getId());

        RoundEntity round = roundRepository.findByGameSessionId(gameSession.getId()).stream()
                .filter(candidate -> candidate.getState() == RoundState.QUESTION_REVEALED)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No active round is available"));
        log.info("Retrieved Round entity with id '{}'", round.getId());
        QuestionEntity question = questionRepository.findById(round.getQuestionId())
                .orElseThrow(() -> new IllegalArgumentException("Question for active round was not found"));
        log.info("Retrieved Question entity with id '{}'", question.getId());
        int assignedCardValue = roundCardAssignmentService.assignedCardValue(room.getId(), round.getId(), playerId);

        log.info("Retrieved active round result for room '{}'", room.getId());
        return new ActiveRoundResult(
                room.getId(),
                room.getCode(),
                gameSession.getId(),
                round.getId(),
                gameSession.getCurrentRoundNumber(),
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
