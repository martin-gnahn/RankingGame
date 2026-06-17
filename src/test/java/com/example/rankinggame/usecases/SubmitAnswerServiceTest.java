package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.SubmitAnswerCommand;
import com.example.rankinggame.dto.SubmitAnswerResult;
import com.example.rankinggame.entities.Answer;
import com.example.rankinggame.entities.GameSession;
import com.example.rankinggame.entities.GameSessionStatus;
import com.example.rankinggame.entities.GameType;
import com.example.rankinggame.entities.Player;
import com.example.rankinggame.entities.PlayerConnectionStatus;
import com.example.rankinggame.entities.Room;
import com.example.rankinggame.entities.RoomStatus;
import com.example.rankinggame.entities.Round;
import com.example.rankinggame.entities.RoundState;
import com.example.rankinggame.repositories.AnswerRepository;
import com.example.rankinggame.repositories.GameSessionRepository;
import com.example.rankinggame.repositories.PlayerRepository;
import com.example.rankinggame.repositories.RoomRepository;
import com.example.rankinggame.repositories.RoundRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubmitAnswerServiceTest {
    @Test
    void savesTrimmedAnswerForCurrentRound() {
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
                answerRepository
        );
        UUID roomId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID gameSessionId = UUID.randomUUID();
        UUID roundId = UUID.randomUUID();
        UUID answerId = UUID.randomUUID();
        Room room = room(roomId);
        Player player = player(playerId, roomId);
        GameSession gameSession = gameSession(gameSessionId, roomId);
        Round round = round(roundId, gameSessionId);
        when(roomRepository.findByCode("ABCD12")).thenReturn(Optional.of(room));
        when(playerRepository.findById(playerId)).thenReturn(Optional.of(player));
        when(roundRepository.findById(roundId)).thenReturn(Optional.of(round));
        when(gameSessionRepository.findByRoomId(roomId)).thenReturn(Optional.of(gameSession));
        when(answerRepository.existsByRoundIdAndPlayerId(roundId, playerId)).thenReturn(false);
        when(answerRepository.save(any(Answer.class))).thenAnswer(invocation -> {
            Answer answer = invocation.getArgument(0);
            answer.setId(answerId);
            return answer;
        });

        SubmitAnswerResult result = service.submitAnswer(new SubmitAnswerCommand(
                " abcd12 ",
                roundId,
                playerId,
                " Mit WLAN-Problemen. ",
                7
        ));

        assertThat(result.answerId()).isEqualTo(answerId);
        assertThat(result.roundId()).isEqualTo(roundId);
        assertThat(result.playerId()).isEqualTo(playerId);
        assertThat(result.submitted()).isTrue();

        ArgumentCaptor<Answer> answerCaptor = ArgumentCaptor.forClass(Answer.class);
        verify(answerRepository).save(answerCaptor.capture());
        assertThat(answerCaptor.getValue().getText()).isEqualTo("Mit WLAN-Problemen.");
        assertThat(answerCaptor.getValue().getCardValue()).isEqualTo(7);
    }

    @Test
    void rejectsDuplicateAnswerForSameRoundAndPlayer() {
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
                answerRepository
        );
        UUID roomId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID gameSessionId = UUID.randomUUID();
        UUID roundId = UUID.randomUUID();
        when(roomRepository.findByCode("ABCD12")).thenReturn(Optional.of(room(roomId)));
        when(playerRepository.findById(playerId)).thenReturn(Optional.of(player(playerId, roomId)));
        when(roundRepository.findById(roundId)).thenReturn(Optional.of(round(roundId, gameSessionId)));
        when(gameSessionRepository.findByRoomId(roomId)).thenReturn(Optional.of(gameSession(gameSessionId, roomId)));
        when(answerRepository.existsByRoundIdAndPlayerId(roundId, playerId)).thenReturn(true);

        assertThatThrownBy(() -> service.submitAnswer(new SubmitAnswerCommand(
                "ABCD12",
                roundId,
                playerId,
                "Mit WLAN-Problemen.",
                7
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Player already submitted an answer for this round");
        verify(answerRepository, never()).save(any());
    }

    private Room room(UUID roomId) {
        Room room = new Room();
        room.setId(roomId);
        room.setCode("ABCD12");
        room.setStatus(RoomStatus.IN_GAME);
        return room;
    }

    private Player player(UUID playerId, UUID roomId) {
        Player player = new Player();
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
        gameSession.setCurrentRoundNumber(1);
        return gameSession;
    }

    private Round round(UUID roundId, UUID gameSessionId) {
        Round round = new Round();
        round.setId(roundId);
        round.setGameSessionId(gameSessionId);
        round.setRoundNumber(1);
        round.setState(RoundState.QUESTION_REVEALED);
        return round;
    }
}
