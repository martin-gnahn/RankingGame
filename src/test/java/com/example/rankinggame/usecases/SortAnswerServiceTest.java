package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.SortAnswersCommand;
import com.example.rankinggame.entities.*;
import com.example.rankinggame.repositories.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SortAnswerServiceTest {


    private static final String ROOM_CODE = "ROOM";
    private static final UUID ROOM_ID = UUID.randomUUID();
    private static final UUID ROUND_ID = UUID.randomUUID();
    private static final UUID HOST_PLAYER_ID = UUID.randomUUID();
    private static final UUID ANSWER_ID = UUID.randomUUID();
    private static final UUID GAME_SESSION_ID = UUID.randomUUID();
    @InjectMocks
    private SortAnswerService sortAnswerService;
    @Mock
    private RoomCodeService roomCodeService;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private RoundRepository roundRepository;
    @Mock
    private GameSessionRepository gameSessionRepository;
    @Mock
    private JpaRankingRepository rankingRepository;
    @Mock
    private JpaAnswerRepository jpaAnswerRepository;

    @Test
    void shouldAddRankingIfAnswerNotRankedYet() {
        // arrange
        GameSession gameSession = new GameSession();
        gameSession.setId(GAME_SESSION_ID);
        gameSession.setCurrentRoundId(ROUND_ID);
        gameSession.setRoomId(ROOM_ID);

        QuestionEntity questionEntity = new QuestionEntity();
        RoundEntity roundEntity =
                new RoundEntity(
                        ROUND_ID, GAME_SESSION_ID, questionEntity,
                        UUID.randomUUID(), RoundState.SORTING, LocalDateTime.now()
                );
        PlayerEntity playerEntity = new PlayerEntity();
        playerEntity.setId(HOST_PLAYER_ID);
        playerEntity.setHost(true);

        RoomEntity roomEntity = new RoomEntity(UUID.randomUUID(), ROOM_CODE, HOST_PLAYER_ID, RoomStatus.IN_GAME, Instant.now());
        when(roomCodeService.normalizeRoomCode(any(SortAnswersCommand.class))).thenReturn(ROOM_CODE);
        when(roomRepository.findByCode(anyString())).thenReturn(Optional.of(roomEntity));
        when(playerRepository.findById(any(UUID.class))).thenReturn(Optional.of(playerEntity));
        when(roundRepository.findById(any(UUID.class))).thenReturn(Optional.of(roundEntity));
        when(gameSessionRepository.findByRoomId(any(UUID.class))).thenReturn(Optional.of(gameSession));
        AnswerEntity answer = new AnswerEntity();
        answer.setId(ANSWER_ID);
        answer.setRoundId(ROUND_ID);
        when(jpaAnswerRepository.findById(any(UUID.class))).thenReturn(Optional.of(answer));
        when(rankingRepository.findByRoundIdAndAnswer(ROUND_ID, answer)).thenReturn(Optional.empty());

        // act
        SortAnswersCommand sortAnswersCommand =
                new SortAnswersCommand(ROOM_CODE, ROUND_ID, HOST_PLAYER_ID, ANSWER_ID);
        sortAnswerService.addRanking(sortAnswersCommand);

        // assert
        int a = 0;
    }

    @Test
    void getOrderOfAnswers() {
    }
}
