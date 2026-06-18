package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.ActiveRoundResult;
import com.example.rankinggame.entities.GameSession;
import com.example.rankinggame.entities.Question;
import com.example.rankinggame.entities.Room;
import com.example.rankinggame.entities.RoomStatus;
import com.example.rankinggame.entities.Round;
import com.example.rankinggame.exceptions.RoomNotFoundException;
import com.example.rankinggame.repositories.GameSessionRepository;
import com.example.rankinggame.repositories.QuestionRepository;
import com.example.rankinggame.repositories.RoomRepository;
import com.example.rankinggame.repositories.RoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class GetActiveRoundService {
    private final RoomRepository roomRepository;
    private final GameSessionRepository gameSessionRepository;
    private final RoundRepository roundRepository;
    private final QuestionRepository questionRepository;

    /// TODO: Why do we need transactional here
    @Transactional(readOnly = true)
    public ActiveRoundResult getActiveRound(String roomCode) {
        String normalizedRoomCode = normalizeRoomCode(roomCode);
        Room room = roomRepository.findByCode(normalizedRoomCode)
                .orElseThrow(() -> new RoomNotFoundException(normalizedRoomCode));

        if (room.getStatus() != RoomStatus.IN_GAME) {
            throw new IllegalArgumentException("No active game is running");
        }

        // TODO: Prefer custom exceptions. maybe no hardcoded error message strings at this service.
        GameSession gameSession = gameSessionRepository.findByRoomId(room.getId())
                .orElseThrow(() -> new IllegalArgumentException("No active game is running"));
        Round round = roundRepository.findByGameSessionId(gameSession.getId()).stream()
                .filter(candidate -> candidate.getRoundNumber() == gameSession.getCurrentRoundNumber())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No active round is available"));
        Question question = questionRepository.findById(round.getQuestionId())
                .orElseThrow(() -> new IllegalArgumentException("Question for active round was not found"));

        return new ActiveRoundResult(
                room.getId(),
                room.getCode(),
                gameSession.getId(),
                round.getId(),
                round.getRoundNumber(),
                question.getId(),
                question.getText()
        );
    }

    private String normalizeRoomCode(String roomCode) {
        if (roomCode == null || roomCode.isBlank()) {
            throw new RoomNotFoundException("");
        }

        return roomCode.trim().toUpperCase(Locale.ROOT);
    }
}
