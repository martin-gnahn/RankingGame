package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.SubmitAnswerCommand;
import com.example.rankinggame.dto.SubmitAnswerResult;
import com.example.rankinggame.engine.PlayerId;
import com.example.rankinggame.engine.Round;
import com.example.rankinggame.engine.RoundId;
import com.example.rankinggame.engine.SubmittedAnswer;
import com.example.rankinggame.engine.exceptions.AnswerAlreadySubmittedException;
import com.example.rankinggame.entities.AnswerEntity;
import com.example.rankinggame.entities.RoomEntity;
import com.example.rankinggame.entities.RoundEntity;
import com.example.rankinggame.events.AnswerSubmittedEvent;
import com.example.rankinggame.events.SortingStartedEvent;
import com.example.rankinggame.mapper.AnswerMapper;
import com.example.rankinggame.mapper.RoundMapper;
import com.example.rankinggame.repositories.AnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubmitAnswerService {
    private final AnswerRepository answerRepository;
    private final RoundCardAssignmentService roundCardAssignmentService;
    private final RoundMapper roundMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AnswerMapper answerMapper;
    private final AnswerSubmissionContextLoader answerSubmissionContextLoader;
    private final RoundProgressService roundProgressService;

    @Transactional
    public SubmitAnswerResult submitAnswer(SubmitAnswerCommand command) {
        ensurePresenceOfRoundIdAndPlayerId(command);
        AnswerSubmissionContext context = answerSubmissionContextLoader.load(command);

        RoundId roundId = new RoundId(context.round().getId());
        PlayerId playerId = new PlayerId(context.player().getId());

        var existingSubmittedAnswers =
                answerRepository.findByRoundIdOrderBySubmittedAtAsc(roundId.value());

        Round domainRound = roundMapper.toDomain(context.round(), existingSubmittedAnswers);
        int cardValue = roundCardAssignmentService.getCardValue(context.room().getId(), roundId.value(), playerId.value());
        SubmittedAnswer submittedAnswer = domainRound.submitAnswer(playerId, command.answerText(), cardValue);

        AnswerEntity answer = answerMapper.toEntity(
                roundId,
                submittedAnswer
        );

        try {
            AnswerEntity savedAnswer = answerRepository.saveAndFlush(answer);
            AnswerSubmissionProgress progress = roundProgressService.updateAfterAnswerSubmitted(context, domainRound);
            publishAnswerSubmittedEvent(context.room(), context.round(), progress);
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
        if (progress.sortingHasStarted()) {
            eventPublisher.publishEvent(new SortingStartedEvent(
                    room.getCode(),
                    round.getId()
            ));
        }
    }
}
