package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.StartRankingGameCommand;
import com.example.rankinggame.dto.StartRankingGameResult;
import com.example.rankinggame.entities.GameSession;
import com.example.rankinggame.entities.GameSessionStatus;
import com.example.rankinggame.entities.GameType;
import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.entities.PlayerConnectionStatus;
import com.example.rankinggame.entities.QuestionEntity;
import com.example.rankinggame.entities.RoomEntity;
import com.example.rankinggame.entities.RoomStatus;
import com.example.rankinggame.entities.RoundEntity;
import com.example.rankinggame.entities.RoundState;
import com.example.rankinggame.events.GameStartedRoomEvent;
import com.example.rankinggame.mapper.GameMapper;
import com.example.rankinggame.mapper.PlayerMapper;
import com.example.rankinggame.mapper.QuestionMapper;
import com.example.rankinggame.mapper.RoundMapper;
import com.example.rankinggame.repositories.GameSessionRepository;
import com.example.rankinggame.repositories.PlayerRepository;
import com.example.rankinggame.repositories.QuestionRepository;
import com.example.rankinggame.repositories.RoomRepository;
import com.example.rankinggame.repositories.RoundRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StartRankingGameServiceTest {
    @Test
    void hostStartsRankingGameFromLobby() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        QuestionRepository questionRepository = mock(QuestionRepository.class);
        GameSessionRepository gameSessionRepository = mock(GameSessionRepository.class);
        RoundRepository roundRepository = mock(RoundRepository.class);
        RoundCardAssignmentService roundCardAssignmentService = mock(RoundCardAssignmentService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        StartRankingGameService service = service(
                roomRepository,
                playerRepository,
                questionRepository,
                gameSessionRepository,
                roundRepository,
                roundCardAssignmentService,
                eventPublisher
        );
        UUID roomId = UUID.randomUUID();
        UUID hostPlayerId = UUID.randomUUID();
        UUID guestPlayerId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID gameSessionId = UUID.randomUUID();
        UUID roundId = UUID.randomUUID();
        RoomEntity room = room(roomId, "ABCD12", hostPlayerId, RoomStatus.LOBBY);
        PlayerEntity hostPlayer = player(hostPlayerId, roomId, true);
        PlayerEntity guestPlayer = player(guestPlayerId, roomId, false);
        QuestionEntity question = question(questionId);
        GameSession savedGameSession = gameSession(gameSessionId, roomId);
        RoundEntity savedRound = round(roundId, gameSessionId, questionId);
        when(roomRepository.findByCode("ABCD12")).thenReturn(Optional.of(room));
        when(playerRepository.findById(hostPlayerId)).thenReturn(Optional.of(hostPlayer));
        when(playerRepository.findByRoomId(roomId)).thenReturn(java.util.List.of(hostPlayer, guestPlayer));
        when(questionRepository.findRandomActive()).thenReturn(Optional.of(question));
        when(gameSessionRepository.save(any(GameSession.class))).thenReturn(savedGameSession);
        when(roundRepository.save(any(RoundEntity.class))).thenReturn(savedRound);
        when(roomRepository.save(room)).thenReturn(room);

        StartRankingGameResult result = service.startGame(new StartRankingGameCommand(" abcd12 ", hostPlayerId));

        assertThat(result.room().id()).isEqualTo(roomId);
        assertThat(result.room().code()).isEqualTo("ABCD12");
        assertThat(result.game().id()).isEqualTo(gameSessionId);
        assertThat(result.game().gameType()).isEqualTo(GameType.RANKING_GAME);
        assertThat(result.round().id()).isEqualTo(roundId);
        assertThat(result.round().number()).isEqualTo(1);
        assertThat(result.round().questionId()).isEqualTo(questionId);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.IN_GAME);

        ArgumentCaptor<GameSession> gameSessionCaptor = ArgumentCaptor.forClass(GameSession.class);
        verify(gameSessionRepository).save(gameSessionCaptor.capture());
        assertThat(gameSessionCaptor.getValue().getRoomId()).isEqualTo(roomId);
        assertThat(gameSessionCaptor.getValue().getGameType()).isEqualTo(GameType.RANKING_GAME);
        assertThat(gameSessionCaptor.getValue().getStatus()).isEqualTo(GameSessionStatus.IN_PROGRESS);
        assertThat(gameSessionCaptor.getValue().getCurrentRoundNumber()).isEqualTo(1);

        ArgumentCaptor<RoundEntity> roundCaptor = ArgumentCaptor.forClass(RoundEntity.class);
        verify(roundRepository).save(roundCaptor.capture());
        assertThat(roundCaptor.getValue().getGameSessionId()).isEqualTo(gameSessionId);
        assertThat(roundCaptor.getValue().getQuestionId()).isEqualTo(questionId);
        assertThat(roundCaptor.getValue().getCaptainPlayerId()).isEqualTo(hostPlayerId);
        assertThat(roundCaptor.getValue().getState()).isEqualTo(RoundState.QUESTION_REVEALED);

        verify(eventPublisher).publishEvent(new GameStartedRoomEvent("ABCD12", gameSessionId, GameType.RANKING_GAME));
        verify(roundCardAssignmentService).assignedCardValue(roomId, roundId, hostPlayerId);
    }

    @Test
    void rejectsStartWhenPlayerIsNotHost() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        QuestionRepository questionRepository = mock(QuestionRepository.class);
        GameSessionRepository gameSessionRepository = mock(GameSessionRepository.class);
        RoundRepository roundRepository = mock(RoundRepository.class);
        RoundCardAssignmentService roundCardAssignmentService = mock(RoundCardAssignmentService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        StartRankingGameService service = service(
                roomRepository,
                playerRepository,
                questionRepository,
                gameSessionRepository,
                roundRepository,
                roundCardAssignmentService,
                eventPublisher
        );
        UUID roomId = UUID.randomUUID();
        UUID hostPlayerId = UUID.randomUUID();
        UUID guestPlayerId = UUID.randomUUID();
        when(roomRepository.findByCode("ABCD12")).thenReturn(Optional.of(room(roomId, "ABCD12", hostPlayerId, RoomStatus.LOBBY)));
        when(playerRepository.findById(guestPlayerId)).thenReturn(Optional.of(player(guestPlayerId, roomId, false)));

        assertThatThrownBy(() -> service.startGame(new StartRankingGameCommand("ABCD12", guestPlayerId)))
                .isInstanceOf(OnlyHostCanStartGame.class)
                .hasMessage("Only the host can start the game");

        verify(gameSessionRepository, never()).save(any());
        verify(roundRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void rejectsStartOutsideLobby() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        QuestionRepository questionRepository = mock(QuestionRepository.class);
        GameSessionRepository gameSessionRepository = mock(GameSessionRepository.class);
        RoundRepository roundRepository = mock(RoundRepository.class);
        RoundCardAssignmentService roundCardAssignmentService = mock(RoundCardAssignmentService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        StartRankingGameService service = service(
                roomRepository,
                playerRepository,
                questionRepository,
                gameSessionRepository,
                roundRepository,
                roundCardAssignmentService,
                eventPublisher
        );
        UUID hostPlayerId = UUID.randomUUID();
        when(roomRepository.findByCode("ABCD12"))
                .thenReturn(Optional.of(room(UUID.randomUUID(), "ABCD12", hostPlayerId, RoomStatus.IN_GAME)));

        assertThatThrownBy(() -> service.startGame(new StartRankingGameCommand("ABCD12", hostPlayerId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Room is not in lobby");

        verify(playerRepository, never()).findById(any());
        verify(gameSessionRepository, never()).save(any());
    }

    @Test
    void rejectsStartWhenOnlyHostIsOnline() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        QuestionRepository questionRepository = mock(QuestionRepository.class);
        GameSessionRepository gameSessionRepository = mock(GameSessionRepository.class);
        RoundRepository roundRepository = mock(RoundRepository.class);
        RoundCardAssignmentService roundCardAssignmentService = mock(RoundCardAssignmentService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        StartRankingGameService service = service(
                roomRepository,
                playerRepository,
                questionRepository,
                gameSessionRepository,
                roundRepository,
                roundCardAssignmentService,
                eventPublisher
        );
        UUID roomId = UUID.randomUUID();
        UUID hostPlayerId = UUID.randomUUID();
        RoomEntity room = room(roomId, "ABCD12", hostPlayerId, RoomStatus.LOBBY);
        PlayerEntity hostPlayer = player(hostPlayerId, roomId, true);
        when(roomRepository.findByCode("ABCD12")).thenReturn(Optional.of(room));
        when(playerRepository.findById(hostPlayerId)).thenReturn(Optional.of(hostPlayer));
        when(playerRepository.findByRoomId(roomId)).thenReturn(java.util.List.of(hostPlayer));

        assertThatThrownBy(() -> service.startGame(new StartRankingGameCommand("ABCD12", hostPlayerId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least two online players are required to start the game");

        verify(questionRepository, never()).findRandomActive();
        verify(gameSessionRepository, never()).save(any());
        verify(roundRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void rejectsStartWhenAllGuestsAreDisconnected() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        QuestionRepository questionRepository = mock(QuestionRepository.class);
        GameSessionRepository gameSessionRepository = mock(GameSessionRepository.class);
        RoundRepository roundRepository = mock(RoundRepository.class);
        RoundCardAssignmentService roundCardAssignmentService = mock(RoundCardAssignmentService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        StartRankingGameService service = service(
                roomRepository,
                playerRepository,
                questionRepository,
                gameSessionRepository,
                roundRepository,
                roundCardAssignmentService,
                eventPublisher
        );
        UUID roomId = UUID.randomUUID();
        UUID hostPlayerId = UUID.randomUUID();
        UUID guestPlayerId = UUID.randomUUID();
        RoomEntity room = room(roomId, "ABCD12", hostPlayerId, RoomStatus.LOBBY);
        PlayerEntity hostPlayer = player(hostPlayerId, roomId, true);
        PlayerEntity guestPlayer = player(guestPlayerId, roomId, false);
        guestPlayer.setConnectionStatus(PlayerConnectionStatus.DISCONNECTED);
        when(roomRepository.findByCode("ABCD12")).thenReturn(Optional.of(room));
        when(playerRepository.findById(hostPlayerId)).thenReturn(Optional.of(hostPlayer));
        when(playerRepository.findByRoomId(roomId)).thenReturn(java.util.List.of(hostPlayer, guestPlayer));

        assertThatThrownBy(() -> service.startGame(new StartRankingGameCommand("ABCD12", hostPlayerId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least two online players are required to start the game");

        verify(questionRepository, never()).findRandomActive();
        verify(gameSessionRepository, never()).save(any());
        verify(roundRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    private RoomEntity room(UUID roomId, String roomCode, UUID hostPlayerId, RoomStatus status) {
        RoomEntity room = new RoomEntity();
        room.setId(roomId);
        room.setCode(roomCode);
        room.setHostPlayerId(hostPlayerId);
        room.setStatus(status);
        return room;
    }

    private PlayerEntity player(UUID playerId, UUID roomId, boolean host) {
        PlayerEntity player = new PlayerEntity();
        player.setId(playerId);
        player.setRoomId(roomId);
        player.setNickname(host ? "Host" : "Guest");
        player.setHost(host);
        player.setConnectionStatus(PlayerConnectionStatus.CONNECTED);
        player.setJoinedAt(Instant.now());
        return player;
    }

    private QuestionEntity question(UUID questionId) {
        QuestionEntity question = new QuestionEntity();
        question.setId(questionId);
        question.setText("Question");
        question.setCategory("test");
        question.setActive(true);
        return question;
    }

    private GameSession gameSession(UUID gameSessionId, UUID roomId) {
        GameSession gameSession = new GameSession();
        gameSession.setId(gameSessionId);
        gameSession.setRoomId(roomId);
        gameSession.setGameType(GameType.RANKING_GAME);
        gameSession.setStatus(GameSessionStatus.IN_PROGRESS);
        gameSession.setCurrentRoundNumber(1);
        return gameSession;
    }

    private RoundEntity round(UUID roundId, UUID gameSessionId, UUID questionId) {
        RoundEntity round = new RoundEntity();
        round.setId(roundId);
        round.setGameSessionId(gameSessionId);
        round.setQuestionId(questionId);
        round.setState(RoundState.QUESTION_REVEALED);
        return round;
    }

    private StartRankingGameService service(
            RoomRepository roomRepository,
            PlayerRepository playerRepository,
            QuestionRepository questionRepository,
            GameSessionRepository gameSessionRepository,
            RoundRepository roundRepository,
            RoundCardAssignmentService roundCardAssignmentService,
            ApplicationEventPublisher eventPublisher
    ) {
        QuestionMapper questionMapper = new QuestionMapper();
        RoundMapper roundMapper = new RoundMapper(questionMapper);
        PlayerMapper playerMapper = new PlayerMapper();
        GameMapper gameMapper = new GameMapper(playerMapper, roundMapper);

        return new StartRankingGameService(
                roomRepository,
                playerRepository,
                questionRepository,
                gameSessionRepository,
                roundRepository,
                roundCardAssignmentService,
                eventPublisher,
                playerMapper,
                gameMapper,
                roundMapper,
                questionMapper,
                new RoomCodeService()
        );
    }
}
