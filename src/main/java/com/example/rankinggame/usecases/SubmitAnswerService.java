package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.SubmitAnswerCommand;
import com.example.rankinggame.dto.SubmitAnswerResult;
import com.example.rankinggame.engine.Answer;
import com.example.rankinggame.engine.PlayerId;
import com.example.rankinggame.engine.Round;
import com.example.rankinggame.engine.exceptions.AnswerAlreadySubmittedException;
import com.example.rankinggame.entities.AnswerEntity;
import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.entities.PlayerConnectionStatus;
import com.example.rankinggame.entities.RoomEntity;
import com.example.rankinggame.entities.RoundEntity;
import com.example.rankinggame.entities.RoundState;
import com.example.rankinggame.events.AnswerSubmittedEvent;
import com.example.rankinggame.exceptions.RoomNotFoundException;
import com.example.rankinggame.mapper.RoundMapper;
import com.example.rankinggame.repositories.AnswerRepository;
import com.example.rankinggame.repositories.GameSessionRepository;
import com.example.rankinggame.repositories.PlayerRepository;
import com.example.rankinggame.repositories.RoomRepository;
import com.example.rankinggame.repositories.RoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubmitAnswerService {
    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final GameSessionRepository gameSessionRepository;
    private final RoundRepository roundRepository;
    private final AnswerRepository answerRepository;
    private final RoundCardAssignmentService roundCardAssignmentService;
    private final RoundMapper roundMapper;
    private final RoomCodeService roomCodeService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public SubmitAnswerResult submitAnswer(SubmitAnswerCommand command) {
        String normalizedRoomCode = roomCodeService.normalizeRoomCode(command);
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

        Round domainRound = roundMapper.toDomain(round);
        domainRound.requireAcceptingAnswers();
        String answerText = Answer.normalizeText(command.answerText());

        int cardValue = roundCardAssignmentService.assignedCardValue(room.getId(), round.getId(), player.getId());

        if (answerRepository.existsByRoundIdAndPlayerId(round.getId(), player.getId())) {
            throw new AnswerAlreadySubmittedException();
        }

        Answer submittedAnswer = domainRound.submitAnswer(new PlayerId(player.getId()), answerText, cardValue);

        AnswerEntity answer = new AnswerEntity();
        answer.setRoundId(round.getId());
        answer.setPlayerId(player.getId());
        answer.setText(submittedAnswer.answerText());
        answer.setCardValue(submittedAnswer.cardValue());

        try {
            AnswerEntity savedAnswer = answerRepository.save(answer);
            AnswerSubmissionProgress progress = updateRoundProgress(room, round);
            publishAnswerSubmitted(room, round, progress);
            return new SubmitAnswerResult(savedAnswer.getId(), round.getId(), player.getId(), true);
        } catch (DataIntegrityViolationException exception) {
            throw new AnswerAlreadySubmittedException(exception);
        }
    }

    private AnswerSubmissionProgress updateRoundProgress(RoomEntity room, RoundEntity round) {
        long connectedPlayerCount = playerRepository.findByRoomId(room.getId()).stream()
                // TODO: error. Players get disconnected.
                .filter(player -> player.getConnectionStatus() == PlayerConnectionStatus.CONNECTED)
                .count();
        long submittedAnswerCount = answerRepository.countByRoundId(round.getId());
        boolean allAnswersSubmitted = connectedPlayerCount > 0 && submittedAnswerCount >= connectedPlayerCount;

        if (allAnswersSubmitted) {
            round.setState(RoundState.SORTING);
            roundRepository.save(round);
        }

        return new AnswerSubmissionProgress(submittedAnswerCount, connectedPlayerCount, allAnswersSubmitted);
    }

    private void publishAnswerSubmitted(
            RoomEntity room,
            RoundEntity round,
            AnswerSubmissionProgress progress
    ) {
        eventPublisher.publishEvent(new AnswerSubmittedEvent(
                room.getCode(),
                round.getId(),
                progress.submittedAnswerCount(),
                progress.requiredAnswerCount(),
                progress.allAnswersSubmitted()
        ));
    }

    private record AnswerSubmissionProgress(
            long submittedAnswerCount,
            long requiredAnswerCount,
            boolean allAnswersSubmitted
    ) {
    }
}
