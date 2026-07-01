package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.SubmitAnswerCommand;
import com.example.rankinggame.dto.SubmitAnswerResult;
import com.example.rankinggame.engine.*;
import com.example.rankinggame.engine.exceptions.AnswerAlreadySubmittedException;
import com.example.rankinggame.entities.*;
import com.example.rankinggame.events.AnswerSubmittedEvent;
import com.example.rankinggame.exceptions.RoomNotFoundException;
import com.example.rankinggame.mapper.AnswerMapper;
import com.example.rankinggame.mapper.RoundMapper;
import com.example.rankinggame.repositories.*;
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
        ensurePresenceOfRoundIdAndPlayerId(command);

        EntityHolder requiredEntities = getRequiredEntities(command, normalizedRoomCode);
        RoundId roundId = new RoundId(requiredEntities.round().getId());
        PlayerId playerId = new PlayerId(requiredEntities.player().getId());

        var otherSubmittedAnswers =
                answerRepository.findByRoundIdOrderBySubmittedAtAsc(roundId.value());

        Round domainRound = roundMapper.toDomain(requiredEntities.round(), otherSubmittedAnswers);
        domainRound.checkIfSubmittingAnswerAllowed();
        String answerTextValue = AnswerText.normalizeText(command.answerText());

        UUID roomId = requiredEntities.room().getId();
        int cardValue = roundCardAssignmentService.getCardValue(roomId, roundId.value(), playerId.value());

        SubmittedAnswer submittedAnswer = domainRound.submitAnswer(playerId, answerTextValue, cardValue);

        AnswerEntity answer = answerMapper.toEntity(
                roundId,
                submittedAnswer
        );

        try {
            AnswerEntity savedAnswer = answerRepository.save(answer);
            AnswerSubmissionProgress progress = updateRoundProgress(requiredEntities.room(), requiredEntities.round());
            publishAnswerSubmittedEvent(requiredEntities.room(), requiredEntities.round(), progress);
            return new SubmitAnswerResult(savedAnswer.getId(), roundId.value(), playerId.value(), true);
        } catch (DataIntegrityViolationException exception) {
            throw new AnswerAlreadySubmittedException(exception);
        }
    }

    private void ensurePresenceOfRoundIdAndPlayerId(SubmitAnswerCommand command) {
        if (command.roundId() == null) {
            throw new RoundIdRequiredException();
        }
        if (command.playerId() == null) {
            throw new PlayerIdRequiredException();
        }
    }

    private EntityHolder getRequiredEntities(SubmitAnswerCommand command, String normalizedRoomCode) {
        RoomEntity room = roomRepository.findByCode(normalizedRoomCode)
                .orElseThrow(() -> new RoomNotFoundException(normalizedRoomCode));
        PlayerEntity player = playerRepository.findById(command.playerId())
                .filter(candidate -> candidate.getRoomId().equals(room.getId()))
                .orElseThrow(PlayerNotInRoomException::new);
        RoundEntity round = roundRepository.findById(command.roundId())
                .orElseThrow(RoundNotPartOfActiveGameException::new);
        EntityHolder requiredEntities = new EntityHolder(room, player, round);
        gameSessionRepository.findByRoomId(requiredEntities.room().getId())
                .filter(candidate -> candidate.getId().equals(requiredEntities.round().getGameSessionId()))
                .orElseThrow(RoundNotPartOfActiveGameException::new);
        return requiredEntities;
    }

    private AnswerSubmissionProgress updateRoundProgress(RoomEntity room, RoundEntity round) {
        long connectedPlayerCount = playerRepository.findByRoomId(room.getId()).stream()
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

    private void publishAnswerSubmittedEvent(
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
