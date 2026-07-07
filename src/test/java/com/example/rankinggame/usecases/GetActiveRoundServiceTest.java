package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.ActiveRoundResult;
import com.example.rankinggame.entities.*;
import com.example.rankinggame.exceptions.ActiveRoundNotFoundException;
import com.example.rankinggame.exceptions.ActiveRoundQuestionNotFoundException;
import com.example.rankinggame.exceptions.RoomHasNoActiveGameException;
import com.example.rankinggame.mapper.GameMapper;
import com.example.rankinggame.repositories.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetActiveRoundServiceTest {
    @Mock
    private RoomRepository roomRepository;

    @Mock
    private GameSessionRepository gameSessionRepository;

    @Mock
    private RoundRepository roundRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private RoundCardAssignmentService roundCardAssignmentService;

    @Mock
    private JpaPlayerRepository playerRepository;

    @Mock
    private GameMapper gameMapper;

    @InjectMocks
    private GetActiveRoundService service;

    @Test
    void returnsQuestionTextForCurrentRound() {
        UUID roomId = UUID.randomUUID();
        UUID gameSessionId = UUID.randomUUID();
        UUID roundId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        RoomEntity room = room(roomId, "ABCD12", RoomStatus.IN_GAME);
        GameSession gameSession = gameSession(gameSessionId, roomId, roundId, 0);
        RoundEntity round = round(roundId, gameSessionId, questionId);
        QuestionEntity question = question(questionId, "Welche Ausrede funktioniert immer?");
        when(roomRepository.findByCode("ABCD12")).thenReturn(Optional.of(room));
        when(gameSessionRepository.findByRoomId(roomId)).thenReturn(Optional.of(gameSession));
        when(roundRepository.findById(roundId)).thenReturn(Optional.of(round));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(roundCardAssignmentService.getCardValue(roomId, roundId, playerId)).thenReturn(7);

        ActiveRoundResult result = service.loadActiveRoundForPlayer(" abcd12 ", playerId);

        assertThat(result.roomId()).isEqualTo(roomId);
        assertThat(result.roomCode()).isEqualTo("ABCD12");
        assertThat(result.gameSessionId()).isEqualTo(gameSessionId);
        assertThat(result.roundId()).isEqualTo(roundId);
        assertThat(result.roundIndex()).isEqualTo(0);
        assertThat(result.questionId()).isEqualTo(questionId);
        assertThat(result.questionText()).isEqualTo("Welche Ausrede funktioniert immer?");
        assertThat(result.assignedCardValue()).isEqualTo(7);
    }

    @Test
    void rejectsLobbyRoomsWithoutActiveGame() {
        when(roomRepository.findByCode("ABCD12")).thenReturn(Optional.of(room(UUID.randomUUID(), "ABCD12", RoomStatus.LOBBY)));

        assertThatThrownBy(() -> service.loadActiveRoundForPlayer("ABCD12", UUID.randomUUID()))
                .isInstanceOf(RoomHasNoActiveGameException.class)
                .hasMessage("Room 'ABCD12' has no active game.");
    }

    @Test
    void rejectsInGameRoomsWithoutGameSession() {
        UUID roomId = UUID.randomUUID();
        when(roomRepository.findByCode("ABCD12")).thenReturn(Optional.of(room(roomId, "ABCD12", RoomStatus.IN_GAME)));
        when(gameSessionRepository.findByRoomId(roomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadActiveRoundForPlayer("ABCD12", UUID.randomUUID()))
                .isInstanceOf(RoomHasNoActiveGameException.class)
                .hasMessage("Room 'ABCD12' has no active game.");
    }

    @Test
    void rejectsGameSessionsWithoutActiveRound() {
        UUID roomId = UUID.randomUUID();
        UUID gameSessionId = UUID.randomUUID();
        UUID missingRoundId = UUID.randomUUID();
        when(roomRepository.findByCode("ABCD12")).thenReturn(Optional.of(room(roomId, "ABCD12", RoomStatus.IN_GAME)));
        when(gameSessionRepository.findByRoomId(roomId)).thenReturn(Optional.of(gameSession(gameSessionId, roomId, missingRoundId, 0)));
        when(roundRepository.findById(missingRoundId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadActiveRoundForPlayer("ABCD12", UUID.randomUUID()))
                .isInstanceOf(ActiveRoundNotFoundException.class)
                .hasMessage("Room 'ABCD12' has no active round.");
    }

    @Test
    void rejectsActiveRoundsWithMissingQuestion() {
        UUID roomId = UUID.randomUUID();
        UUID gameSessionId = UUID.randomUUID();
        UUID roundId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        when(roomRepository.findByCode("ABCD12")).thenReturn(Optional.of(room(roomId, "ABCD12", RoomStatus.IN_GAME)));
        when(gameSessionRepository.findByRoomId(roomId)).thenReturn(Optional.of(gameSession(gameSessionId, roomId, roundId, 0)));
        when(roundRepository.findById(roundId)).thenReturn(Optional.of(round(roundId, gameSessionId, questionId)));
        when(questionRepository.findById(questionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadActiveRoundForPlayer("ABCD12", UUID.randomUUID()))
                .isInstanceOf(ActiveRoundQuestionNotFoundException.class)
                .hasMessage("Question '%s' for active round '%s' was not found.", questionId, roundId);
    }

    @Test
    void activeRoundLookupAllowsCardAssignmentWrites() throws NoSuchMethodException {
        Method method = GetActiveRoundService.class.getMethod("loadActiveRoundForPlayer", String.class, UUID.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isFalse();
    }

    private RoomEntity room(UUID roomId, String code, RoomStatus status) {
        RoomEntity room = new RoomEntity();
        room.setId(roomId);
        room.setCode(code);
        room.setStatus(status);
        return room;
    }

    private GameSession gameSession(UUID gameSessionId, UUID roomId, UUID currentRoundId, int currentRoundIndex) {
        GameSession gameSession = new GameSession();
        gameSession.setId(gameSessionId);
        gameSession.setRoomId(roomId);
        gameSession.setCurrentRoundId(currentRoundId);
        gameSession.setGameType(GameType.RANKING_GAME);
        gameSession.setStatus(GameSessionStatus.IN_PROGRESS);
        gameSession.setCurrentRoundIndex(currentRoundIndex);
        return gameSession;
    }

    private RoundEntity round(UUID roundId, UUID gameSessionId, UUID questionId) {
        RoundEntity round = new RoundEntity();
        round.setId(roundId);
        round.setGameSessionId(gameSessionId);
        round.setQuestionId(questionId);
        round.setState(RoundState.ANSWER_SUBMISSION);
        return round;
    }

    private QuestionEntity question(UUID questionId, String text) {
        QuestionEntity question = new QuestionEntity();
        question.setId(questionId);
        question.setText(text);
        question.setCategory("test");
        question.setActive(true);
        return question;
    }
}
