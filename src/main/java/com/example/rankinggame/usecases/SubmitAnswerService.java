package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.SubmitAnswerCommand;
import com.example.rankinggame.dto.SubmitAnswerResult;
import com.example.rankinggame.entities.AnswerEntity;
import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.entities.RoomEntity;
import com.example.rankinggame.entities.RoundEntity;
import com.example.rankinggame.entities.RoundState;
import com.example.rankinggame.exceptions.RoomNotFoundException;
import com.example.rankinggame.repositories.AnswerRepository;
import com.example.rankinggame.repositories.GameSessionRepository;
import com.example.rankinggame.repositories.PlayerRepository;
import com.example.rankinggame.repositories.RoomRepository;
import com.example.rankinggame.repositories.RoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SubmitAnswerService {
    private static final int MAX_ANSWER_LENGTH = 500;
    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final GameSessionRepository gameSessionRepository;
    private final RoundRepository roundRepository;
    private final AnswerRepository answerRepository;
    private final RoundCardAssignmentService roundCardAssignmentService;

    // TODO: extract game logic to other service
    @Transactional
    public SubmitAnswerResult submitAnswer(SubmitAnswerCommand command) {
        String normalizedRoomCode = normalizeRoomCode(command.roomCode());
        if (command.roundId() == null) {
            throw new RoundIdRequiredException();
        }
        if (command.playerId() == null) {
            throw new PlayerIdRequiredException();
        }

        // TODO: extract to other service
        RoomEntity room = roomRepository.findByCode(normalizedRoomCode)
                .orElseThrow(() -> new RoomNotFoundException(normalizedRoomCode));
        PlayerEntity player = playerRepository.findById(command.playerId())
                .filter(candidate -> candidate.getRoomId().equals(room.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Player is not part of this room"));
        RoundEntity round = roundRepository.findById(command.roundId())
                .orElseThrow(() -> new IllegalArgumentException("Round is not part of the active game"));
        gameSessionRepository.findByRoomId(room.getId())
                .filter(candidate -> candidate.getId().equals(round.getGameSessionId()))
                .orElseThrow(() -> new IllegalArgumentException("Round is not part of the active game"));

        if (round.getState() != RoundState.QUESTION_REVEALED) {
            throw new IllegalArgumentException("Answers are not accepted for this round");
        }

        String answerText = normalizeAnswerText(command.answerText());
        int cardValue = roundCardAssignmentService.assignedCardValue(room.getId(), round.getId(), player.getId());

        if (answerRepository.existsByRoundIdAndPlayerId(round.getId(), player.getId())) {
            throw new IllegalArgumentException("Player already submitted an answer for this round");
        }

        AnswerEntity answer = new AnswerEntity();
        answer.setRoundId(round.getId());
        answer.setPlayerId(player.getId());
        answer.setText(answerText);
        answer.setCardValue(cardValue);

        try {
            AnswerEntity savedAnswer = answerRepository.save(answer);
            return new SubmitAnswerResult(savedAnswer.getId(), round.getId(), player.getId(), true);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("Player already submitted an answer for this round", exception);
        }
    }

    private String normalizeRoomCode(String roomCode) {
        if (roomCode == null || roomCode.isBlank()) {
            throw new RoomNotFoundException("");
        }

        return roomCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeAnswerText(String answerText) {
        if (answerText == null || answerText.isBlank()) {
            throw new IllegalArgumentException("Answer text is required");
        }

        String trimmedAnswerText = answerText.trim();
        if (trimmedAnswerText.length() > MAX_ANSWER_LENGTH) {
            throw new IllegalArgumentException("Answer text must be 500 characters or fewer");
        }

        return trimmedAnswerText;
    }
}
