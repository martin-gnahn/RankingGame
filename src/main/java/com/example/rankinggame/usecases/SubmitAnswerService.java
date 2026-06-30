package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.SubmitAnswerCommand;
import com.example.rankinggame.dto.SubmitAnswerResult;
import com.example.rankinggame.engine.*;
import com.example.rankinggame.engine.exceptions.AnswerAlreadySubmittedException;
import com.example.rankinggame.entities.AnswerEntity;
import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.entities.PlayerConnectionStatus;
import com.example.rankinggame.entities.RoomEntity;
import com.example.rankinggame.entities.RoundEntity;
import com.example.rankinggame.entities.RoundState;
import com.example.rankinggame.events.AnswerSubmittedEvent;
import com.example.rankinggame.exceptions.RoomNotFoundException;
import com.example.rankinggame.mapper.AnswerMapper;
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

import java.util.UUID;

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
    private final AnswerMapper answerMapper;

    /**
     * Answer submission by a specific user.
     * @param command
     * @return
     */
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
        EntityHolder requiredEntities = getRequiredEntities(command, normalizedRoomCode);
        RoundId roundId = new RoundId(requiredEntities.round().getId());
        PlayerId playerId = new PlayerId(requiredEntities.player().getId());

        var otherSubmittedAnswers =
                answerRepository.findByRoundIdOrderBySubmittedAtAsc(roundId.value());

        // TODO: fix that
        Round domainRound = roundMapper.toDomain(requiredEntities.round(), otherSubmittedAnswers);
        domainRound.checkIfSubmittingAnswerAllowed();
        String answerTextValue = AnswerText.normalizeText(command.answerText());

        UUID roomId =requiredEntities.room().getId();
        int cardValue = roundCardAssignmentService.getCardValue(roomId, roundId.value(), playerId.value());

        SubmittedAnswer submittedAnswer = domainRound.submitAnswer(playerId, answerTextValue, cardValue);

        AnswerEntity answer = answerMapper.toEntity(
                roundId,
                submittedAnswer
        );

        try {
            AnswerEntity savedAnswer = answerRepository.save(answer);
            AnswerSubmissionProgress progress = updateRoundProgress(requiredEntities.room(), requiredEntities.round());
            publishAnswerSubmitted(requiredEntities.room(), requiredEntities.round(), progress);
            return new SubmitAnswerResult(savedAnswer.getId(), roundId.value(), playerId.value(), true);
        } catch (DataIntegrityViolationException exception) {
            throw new AnswerAlreadySubmittedException(exception);
        }
    }

    private EntityHolder getRequiredEntities(SubmitAnswerCommand command, String normalizedRoomCode) {
        RoomEntity room = roomRepository.findByCode(normalizedRoomCode)
                .orElseThrow(() -> new RoomNotFoundException(normalizedRoomCode));
        PlayerEntity player = playerRepository.findById(command.playerId())
                .filter(candidate -> candidate.getRoomId().equals(room.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Player is not part of this room"));
        RoundEntity round = roundRepository.findById(command.roundId())
                .orElseThrow(() -> new IllegalArgumentException("Round is not part of the active game"));
        EntityHolder requiredEntities = new EntityHolder(room, player, round);
        gameSessionRepository.findByRoomId(requiredEntities.room().getId())
                .filter(candidate -> candidate.getId().equals(requiredEntities.round().getGameSessionId()))
                .orElseThrow(() -> new IllegalArgumentException("Round is not part of the active game"));
        return requiredEntities;
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

    private record EntityHolder(RoomEntity room, PlayerEntity player, RoundEntity round) {
    }
}
