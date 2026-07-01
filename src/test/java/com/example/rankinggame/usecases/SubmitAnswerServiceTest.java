package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.SubmitAnswerCommand;
import com.example.rankinggame.dto.SubmitAnswerResult;
import com.example.rankinggame.engine.exceptions.AnswerAlreadySubmittedException;
import com.example.rankinggame.entities.*;
import com.example.rankinggame.events.AnswerSubmittedEvent;
import com.example.rankinggame.mapper.AnswerMapper;
import com.example.rankinggame.mapper.QuestionMapper;
import com.example.rankinggame.mapper.RoundMapper;
import com.example.rankinggame.repositories.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SubmitAnswerServiceTest {
    @Test
    void savesTrimmedAnswerForCurrentRound() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        GameSessionRepository gameSessionRepository = mock(GameSessionRepository.class);
        RoundRepository roundRepository = mock(RoundRepository.class);
        AnswerRepository answerRepository = mock(AnswerRepository.class);
        RoundCardAssignmentService roundCardAssignmentService = mock(RoundCardAssignmentService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        SubmitAnswerService service = new SubmitAnswerService(
                roomRepository,
                playerRepository,
                gameSessionRepository,
                roundRepository,
                answerRepository,
                roundCardAssignmentService,
                roundMapper(),
                new RoomCodeService(),
                eventPublisher,
                new AnswerMapper()
        );
        UUID roomId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID gameSessionId = UUID.randomUUID();
        UUID roundId = UUID.randomUUID();
        UUID answerId = UUID.randomUUID();
        RoomEntity room = room(roomId);
        PlayerEntity player = player(playerId, roomId);
        GameSession gameSession = gameSession(gameSessionId, roomId);
        RoundEntity round = round(roundId, gameSessionId);
        when(roomRepository.findByCode("ABCD12")).thenReturn(Optional.of(room));
        when(playerRepository.findById(playerId)).thenReturn(Optional.of(player));
        when(roundRepository.findById(roundId)).thenReturn(Optional.of(round));
        when(gameSessionRepository.findByRoomId(roomId)).thenReturn(Optional.of(gameSession));
        when(roundCardAssignmentService.getCardValue(roomId, roundId, playerId)).thenReturn(7);
        when(answerRepository.existsByRoundIdAndPlayerId(roundId, playerId)).thenReturn(false);
        when(playerRepository.findByRoomId(roomId)).thenReturn(java.util.List.of(player));
        when(answerRepository.countByRoundId(roundId)).thenReturn(1L);
        when(answerRepository.save(any(AnswerEntity.class))).thenAnswer(invocation -> {
            AnswerEntity answer = invocation.getArgument(0);
            answer.setId(answerId);
            return answer;
        });

        SubmitAnswerResult result = service.submitAnswer(new SubmitAnswerCommand(
                " abcd12 ",
                roundId,
                playerId,
                " Mit WLAN-Problemen. "
        ));

        assertThat(result.answerId()).isEqualTo(answerId);
        assertThat(result.roundId()).isEqualTo(roundId);
        assertThat(result.playerId()).isEqualTo(playerId);
        assertThat(result.submitted()).isTrue();

        ArgumentCaptor<AnswerEntity> answerCaptor = ArgumentCaptor.forClass(AnswerEntity.class);
        verify(answerRepository).save(answerCaptor.capture());
        assertThat(answerCaptor.getValue().getText()).isEqualTo("Mit WLAN-Problemen.");
        assertThat(answerCaptor.getValue().getCardValue()).isEqualTo(7);
        assertThat(round.getState()).isEqualTo(RoundState.SORTING);
        verify(roundRepository).save(round);

        ArgumentCaptor<AnswerSubmittedEvent> eventCaptor = ArgumentCaptor.forClass(AnswerSubmittedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isEqualTo(new AnswerSubmittedEvent(
                "ABCD12",
                roundId,
                1,
                1,
                true
        ));
    }

    @Test
    void rejectsDuplicateAnswerForSameRoundAndPlayer() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        GameSessionRepository gameSessionRepository = mock(GameSessionRepository.class);
        RoundRepository roundRepository = mock(RoundRepository.class);
        AnswerRepository answerRepository = mock(AnswerRepository.class);
        RoundCardAssignmentService roundCardAssignmentService = mock(RoundCardAssignmentService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        SubmitAnswerService service = new SubmitAnswerService(
                roomRepository,
                playerRepository,
                gameSessionRepository,
                roundRepository,
                answerRepository,
                roundCardAssignmentService,
                roundMapper(),
                new RoomCodeService(),
                eventPublisher,
                new AnswerMapper()
        );
        UUID roomId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID gameSessionId = UUID.randomUUID();
        UUID roundId = UUID.randomUUID();
        GameSession gameSession = gameSession(gameSessionId, roomId);
        when(roomRepository.findByCode("ABCD12")).thenReturn(Optional.of(room(roomId)));
        when(playerRepository.findById(playerId)).thenReturn(Optional.of(player(playerId, roomId)));
        when(roundRepository.findById(roundId)).thenReturn(Optional.of(round(roundId, gameSessionId)));
        when(gameSessionRepository.findByRoomId(roomId)).thenReturn(Optional.of(gameSession));
        when(roundCardAssignmentService.getCardValue(roomId, roundId, playerId)).thenReturn(7);
        when(answerRepository.existsByRoundIdAndPlayerId(roundId, playerId)).thenReturn(true);
        AnswerEntity answerEntity =
                new AnswerEntity(
                        UUID.randomUUID(), roundId, playerId, "Mit WLAN-Problemen.", 10, Instant.now()
                );
        when(answerRepository.findByRoundIdOrderBySubmittedAtAsc(roundId))
                .thenReturn(List.of(answerEntity));

        assertThatThrownBy(() -> service.submitAnswer(new SubmitAnswerCommand(
                "ABCD12",
                roundId,
                playerId,
                "Mit WLAN-Problemen."
        )))
                .isInstanceOf(AnswerAlreadySubmittedException.class)
                .hasMessage("Player already submitted an answer for this round");
        verify(answerRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void rejectsMissingRoundId() {
        SubmitAnswerService service = serviceWithMocks();

        assertThatThrownBy(() -> service.submitAnswer(new SubmitAnswerCommand(
                "ABCD12",
                null,
                UUID.randomUUID(),
                "Mit WLAN-Problemen."
        )))
                .isInstanceOf(RoundIdRequiredException.class)
                .hasMessage("Round id is required");
    }

    @Test
    void rejectsMissingPlayerId() {
        SubmitAnswerService service = serviceWithMocks();

        assertThatThrownBy(() -> service.submitAnswer(new SubmitAnswerCommand(
                "ABCD12",
                UUID.randomUUID(),
                null,
                "Mit WLAN-Problemen."
        )))
                .isInstanceOf(PlayerIdRequiredException.class)
                .hasMessage("Player id is required");
    }

    @Test
    void rejectsPlayerOutsideRoomWithDomainException() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        GameSessionRepository gameSessionRepository = mock(GameSessionRepository.class);
        RoundRepository roundRepository = mock(RoundRepository.class);
        AnswerRepository answerRepository = mock(AnswerRepository.class);
        SubmitAnswerService service = new SubmitAnswerService(
                roomRepository,
                playerRepository,
                gameSessionRepository,
                roundRepository,
                answerRepository,
                mock(RoundCardAssignmentService.class),
                roundMapper(),
                new RoomCodeService(),
                mock(ApplicationEventPublisher.class),
                new AnswerMapper()
        );
        UUID roomId = UUID.randomUUID();
        UUID otherRoomId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID roundId = UUID.randomUUID();
        when(roomRepository.findByCode("ABCD12")).thenReturn(Optional.of(room(roomId)));
        when(playerRepository.findById(playerId)).thenReturn(Optional.of(player(playerId, otherRoomId)));

        assertThatThrownBy(() -> service.submitAnswer(new SubmitAnswerCommand(
                "ABCD12",
                roundId,
                playerId,
                "Mit WLAN-Problemen."
        )))
                .isInstanceOf(PlayerNotInRoomException.class)
                .hasMessage("Player is not part of this room");
        verify(roundRepository, never()).findById(any());
        verify(answerRepository, never()).save(any());
    }

    @Test
    void rejectsRoundOutsideActiveGameWithDomainException() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        GameSessionRepository gameSessionRepository = mock(GameSessionRepository.class);
        RoundRepository roundRepository = mock(RoundRepository.class);
        AnswerRepository answerRepository = mock(AnswerRepository.class);
        SubmitAnswerService service = new SubmitAnswerService(
                roomRepository,
                playerRepository,
                gameSessionRepository,
                roundRepository,
                answerRepository,
                mock(RoundCardAssignmentService.class),
                roundMapper(),
                new RoomCodeService(),
                mock(ApplicationEventPublisher.class),
                new AnswerMapper()
        );
        UUID roomId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID activeGameSessionId = UUID.randomUUID();
        UUID otherGameSessionId = UUID.randomUUID();
        UUID roundId = UUID.randomUUID();
        when(roomRepository.findByCode("ABCD12")).thenReturn(Optional.of(room(roomId)));
        when(playerRepository.findById(playerId)).thenReturn(Optional.of(player(playerId, roomId)));
        when(roundRepository.findById(roundId)).thenReturn(Optional.of(round(roundId, otherGameSessionId)));
        when(gameSessionRepository.findByRoomId(roomId)).thenReturn(Optional.of(gameSession(activeGameSessionId, roomId)));

        assertThatThrownBy(() -> service.submitAnswer(new SubmitAnswerCommand(
                "ABCD12",
                roundId,
                playerId,
                "Mit WLAN-Problemen."
        )))
                .isInstanceOf(RoundNotPartOfActiveGameException.class)
                .hasMessage("Round is not part of the active game");
        verify(answerRepository, never()).save(any());
    }

    private SubmitAnswerService serviceWithMocks() {
        return new SubmitAnswerService(
                mock(RoomRepository.class),
                mock(PlayerRepository.class),
                mock(GameSessionRepository.class),
                mock(RoundRepository.class),
                mock(AnswerRepository.class),
                mock(RoundCardAssignmentService.class),
                roundMapper(),
                new RoomCodeService(),
                mock(ApplicationEventPublisher.class),
                mock(AnswerMapper.class)
        );
    }

    private RoundMapper roundMapper() {
        return new RoundMapper(new QuestionMapper(), new AnswerMapper());
    }

    private RoomEntity room(UUID roomId) {
        RoomEntity room = new RoomEntity();
        room.setId(roomId);
        room.setCode("ABCD12");
        room.setStatus(RoomStatus.IN_GAME);
        return room;
    }

    private PlayerEntity player(UUID playerId, UUID roomId) {
        PlayerEntity player = new PlayerEntity();
        player.setId(playerId);
        player.setRoomId(roomId);
        player.setNickname("Marta");
        player.setConnectionStatus(PlayerConnectionStatus.CONNECTED);
        return player;
    }

    private GameSession gameSession(UUID gameSessionId, UUID roomId) {
        GameSession gameSession = new GameSession();
        gameSession.setId(gameSessionId);
        gameSession.setRoomId(roomId);
        gameSession.setGameType(GameType.RANKING_GAME);
        gameSession.setStatus(GameSessionStatus.IN_PROGRESS);
        gameSession.setCurrentRoundIndex(0);
        return gameSession;
    }

    private RoundEntity round(UUID roundId, UUID gameSessionId) {
        RoundEntity round = new RoundEntity();
        round.setId(roundId);
        round.setGameSessionId(gameSessionId);
        round.setState(RoundState.ANSWER_SUBMISSION);
        return round;
    }
}
