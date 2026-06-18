package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.ActiveRoundResult;
import com.example.rankinggame.entities.GameSession;
import com.example.rankinggame.entities.GameSessionStatus;
import com.example.rankinggame.entities.GameType;
import com.example.rankinggame.entities.Question;
import com.example.rankinggame.entities.Room;
import com.example.rankinggame.entities.RoomStatus;
import com.example.rankinggame.entities.Round;
import com.example.rankinggame.entities.RoundState;
import com.example.rankinggame.repositories.GameSessionRepository;
import com.example.rankinggame.repositories.QuestionRepository;
import com.example.rankinggame.repositories.RoomRepository;
import com.example.rankinggame.repositories.RoundRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetActiveRoundServiceTest {
    @Test
    void returnsQuestionTextForCurrentRound() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        GameSessionRepository gameSessionRepository = mock(GameSessionRepository.class);
        RoundRepository roundRepository = mock(RoundRepository.class);
        QuestionRepository questionRepository = mock(QuestionRepository.class);
        RoundCardAssignmentService roundCardAssignmentService = mock(RoundCardAssignmentService.class);
        GetActiveRoundService service = new GetActiveRoundService(
                roomRepository,
                gameSessionRepository,
                roundRepository,
                questionRepository,
                roundCardAssignmentService
        );
        UUID roomId = UUID.randomUUID();
        UUID gameSessionId = UUID.randomUUID();
        UUID roundId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        Room room = room(roomId, "ABCD12", RoomStatus.IN_GAME);
        GameSession gameSession = gameSession(gameSessionId, roomId, 1);
        Round round = round(roundId, gameSessionId, questionId, 1);
        Question question = question(questionId, "Welche Ausrede funktioniert immer?");
        when(roomRepository.findByCode("ABCD12")).thenReturn(Optional.of(room));
        when(gameSessionRepository.findByRoomId(roomId)).thenReturn(Optional.of(gameSession));
        when(roundRepository.findByGameSessionId(gameSessionId)).thenReturn(List.of(round));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(roundCardAssignmentService.assignedCardValue(roomId, roundId, playerId)).thenReturn(7);

        ActiveRoundResult result = service.getActiveRound(" abcd12 ", playerId);

        assertThat(result.roomId()).isEqualTo(roomId);
        assertThat(result.roomCode()).isEqualTo("ABCD12");
        assertThat(result.gameSessionId()).isEqualTo(gameSessionId);
        assertThat(result.roundId()).isEqualTo(roundId);
        assertThat(result.roundNumber()).isEqualTo(1);
        assertThat(result.questionId()).isEqualTo(questionId);
        assertThat(result.questionText()).isEqualTo("Welche Ausrede funktioniert immer?");
        assertThat(result.assignedCardValue()).isEqualTo(7);
    }

    @Test
    void rejectsLobbyRoomsWithoutActiveGame() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        GetActiveRoundService service = new GetActiveRoundService(
                roomRepository,
                mock(GameSessionRepository.class),
                mock(RoundRepository.class),
                mock(QuestionRepository.class),
                mock(RoundCardAssignmentService.class)
        );
        when(roomRepository.findByCode("ABCD12")).thenReturn(Optional.of(room(UUID.randomUUID(), "ABCD12", RoomStatus.LOBBY)));

        assertThatThrownBy(() -> service.getActiveRound("ABCD12", UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No active game is running");
    }

    private Room room(UUID roomId, String code, RoomStatus status) {
        Room room = new Room();
        room.setId(roomId);
        room.setCode(code);
        room.setStatus(status);
        return room;
    }

    private GameSession gameSession(UUID gameSessionId, UUID roomId, int currentRoundNumber) {
        GameSession gameSession = new GameSession();
        gameSession.setId(gameSessionId);
        gameSession.setRoomId(roomId);
        gameSession.setGameType(GameType.RANKING_GAME);
        gameSession.setStatus(GameSessionStatus.IN_PROGRESS);
        gameSession.setCurrentRoundNumber(currentRoundNumber);
        return gameSession;
    }

    private Round round(UUID roundId, UUID gameSessionId, UUID questionId, int roundNumber) {
        Round round = new Round();
        round.setId(roundId);
        round.setGameSessionId(gameSessionId);
        round.setQuestionId(questionId);
        round.setRoundNumber(roundNumber);
        round.setState(RoundState.QUESTION_REVEALED);
        return round;
    }

    private Question question(UUID questionId, String text) {
        Question question = new Question();
        question.setId(questionId);
        question.setText(text);
        question.setCategory("test");
        question.setActive(true);
        return question;
    }
}
