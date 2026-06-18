package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.StartRankingGameCommand;
import com.example.rankinggame.dto.StartRankingGameResult;
import com.example.rankinggame.entities.GameSession;
import com.example.rankinggame.entities.GameSessionStatus;
import com.example.rankinggame.entities.GameType;
import com.example.rankinggame.entities.Player;
import com.example.rankinggame.entities.PlayerConnectionStatus;
import com.example.rankinggame.entities.Question;
import com.example.rankinggame.entities.Room;
import com.example.rankinggame.entities.RoomStatus;
import com.example.rankinggame.entities.Round;
import com.example.rankinggame.entities.RoundState;
import com.example.rankinggame.events.GameStartedRoomEvent;
import com.example.rankinggame.repositories.GameSessionRepository;
import com.example.rankinggame.repositories.PlayerRepository;
import com.example.rankinggame.repositories.QuestionRepository;
import com.example.rankinggame.repositories.RoomRepository;
import com.example.rankinggame.repositories.RoundRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

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
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        StartRankingGameService service = new StartRankingGameService(
                roomRepository,
                playerRepository,
                questionRepository,
                gameSessionRepository,
                roundRepository,
                eventPublisher
        );
        UUID roomId = UUID.randomUUID();
        UUID hostPlayerId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID gameSessionId = UUID.randomUUID();
        UUID roundId = UUID.randomUUID();
        Room room = room(roomId, "ABCD12", hostPlayerId, RoomStatus.LOBBY);
        Player hostPlayer = player(hostPlayerId, roomId, true);
        Question question = question(questionId);
        GameSession savedGameSession = gameSession(gameSessionId, roomId);
        Round savedRound = round(roundId, gameSessionId, questionId);
        when(roomRepository.findByCode("ABCD12")).thenReturn(Optional.of(room));
        when(playerRepository.findById(hostPlayerId)).thenReturn(Optional.of(hostPlayer));
        when(questionRepository.findRandomActive()).thenReturn(Optional.of(question));
        when(gameSessionRepository.save(any(GameSession.class))).thenReturn(savedGameSession);
        when(roundRepository.save(any(Round.class))).thenReturn(savedRound);
        when(roomRepository.save(room)).thenReturn(room);

        StartRankingGameResult result = service.startGame(new StartRankingGameCommand(" abcd12 ", hostPlayerId));

        assertThat(result.roomId()).isEqualTo(roomId);
        assertThat(result.roomCode()).isEqualTo("ABCD12");
        assertThat(result.gameSessionId()).isEqualTo(gameSessionId);
        assertThat(result.gameType()).isEqualTo(GameType.RANKING_GAME);
        assertThat(result.roundId()).isEqualTo(roundId);
        assertThat(result.roundNumber()).isEqualTo(1);
        assertThat(result.questionId()).isEqualTo(questionId);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.IN_GAME);

        ArgumentCaptor<GameSession> gameSessionCaptor = ArgumentCaptor.forClass(GameSession.class);
        verify(gameSessionRepository).save(gameSessionCaptor.capture());
        assertThat(gameSessionCaptor.getValue().getRoomId()).isEqualTo(roomId);
        assertThat(gameSessionCaptor.getValue().getGameType()).isEqualTo(GameType.RANKING_GAME);
        assertThat(gameSessionCaptor.getValue().getStatus()).isEqualTo(GameSessionStatus.IN_PROGRESS);
        assertThat(gameSessionCaptor.getValue().getCurrentRoundNumber()).isEqualTo(1);

        ArgumentCaptor<Round> roundCaptor = ArgumentCaptor.forClass(Round.class);
        verify(roundRepository).save(roundCaptor.capture());
        assertThat(roundCaptor.getValue().getGameSessionId()).isEqualTo(gameSessionId);
        assertThat(roundCaptor.getValue().getQuestionId()).isEqualTo(questionId);
        assertThat(roundCaptor.getValue().getRoundNumber()).isEqualTo(1);
        assertThat(roundCaptor.getValue().getState()).isEqualTo(RoundState.QUESTION_REVEALED);

        verify(eventPublisher).publishEvent(new GameStartedRoomEvent("ABCD12", gameSessionId, GameType.RANKING_GAME));
    }

    @Test
    void rejectsStartWhenPlayerIsNotHost() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        QuestionRepository questionRepository = mock(QuestionRepository.class);
        GameSessionRepository gameSessionRepository = mock(GameSessionRepository.class);
        RoundRepository roundRepository = mock(RoundRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        StartRankingGameService service = new StartRankingGameService(
                roomRepository,
                playerRepository,
                questionRepository,
                gameSessionRepository,
                roundRepository,
                eventPublisher
        );
        UUID roomId = UUID.randomUUID();
        UUID hostPlayerId = UUID.randomUUID();
        UUID guestPlayerId = UUID.randomUUID();
        when(roomRepository.findByCode("ABCD12")).thenReturn(Optional.of(room(roomId, "ABCD12", hostPlayerId, RoomStatus.LOBBY)));
        when(playerRepository.findById(guestPlayerId)).thenReturn(Optional.of(player(guestPlayerId, roomId, false)));

        assertThatThrownBy(() -> service.startGame(new StartRankingGameCommand("ABCD12", guestPlayerId)))
                .isInstanceOf(IllegalArgumentException.class)
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
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        StartRankingGameService service = new StartRankingGameService(
                roomRepository,
                playerRepository,
                questionRepository,
                gameSessionRepository,
                roundRepository,
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

    private Room room(UUID roomId, String roomCode, UUID hostPlayerId, RoomStatus status) {
        Room room = new Room();
        room.setId(roomId);
        room.setCode(roomCode);
        room.setHostPlayerId(hostPlayerId);
        room.setStatus(status);
        return room;
    }

    private Player player(UUID playerId, UUID roomId, boolean host) {
        Player player = new Player();
        player.setId(playerId);
        player.setRoomId(roomId);
        player.setNickname(host ? "Host" : "Guest");
        player.setHost(host);
        player.setConnectionStatus(PlayerConnectionStatus.CONNECTED);
        return player;
    }

    private Question question(UUID questionId) {
        Question question = new Question();
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

    private Round round(UUID roundId, UUID gameSessionId, UUID questionId) {
        Round round = new Round();
        round.setId(roundId);
        round.setGameSessionId(gameSessionId);
        round.setQuestionId(questionId);
        round.setRoundNumber(1);
        round.setState(RoundState.QUESTION_REVEALED);
        return round;
    }
}
