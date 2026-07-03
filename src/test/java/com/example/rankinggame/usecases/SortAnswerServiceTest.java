package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.SortAnswersCommand;
import com.example.rankinggame.entities.*;
import com.example.rankinggame.repositories.*;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

    private static @NonNull GameSession getGameSessionEntity() {
        GameSession gameSession = new GameSession();
        gameSession.setId(GAME_SESSION_ID);
        gameSession.setCurrentRoundId(ROUND_ID);
        gameSession.setRoomId(ROOM_ID);
        return gameSession;
    }

    private static @NonNull RoundEntity getRoundEntity(QuestionEntity questionEntity) {
        RoundEntity roundEntity =
                new RoundEntity(
                        ROUND_ID, GAME_SESSION_ID, questionEntity,
                        UUID.randomUUID(), RoundState.SORTING, LocalDateTime.now()
                );
        return roundEntity;
    }

    private static @NonNull AnswerEntity getAnswerEntity() {
        AnswerEntity answer = new AnswerEntity();
        answer.setId(ANSWER_ID);
        answer.setRoundId(ROUND_ID);
        return answer;
    }

    private static @NonNull RoomEntity getRoomEntity() {
        return new RoomEntity(ROOM_ID, ROOM_CODE, HOST_PLAYER_ID, RoomStatus.IN_GAME, Instant.now());
    }

    private static @NonNull PlayerEntity getPlayerEntity() {
        // TODO: currently some duplicate ids here (player has room id and room has host player id: redundancy)
        PlayerEntity playerEntity = new PlayerEntity();
        playerEntity.setId(HOST_PLAYER_ID);
        playerEntity.setHost(true);
        playerEntity.setRoomId(ROOM_ID);
        return playerEntity;
    }

    @Test
    void shouldAddRankingIfAnswerNotRankedYet() {
        // arrange
        GameSession gameSession = getGameSessionEntity();
        QuestionEntity questionEntity = new QuestionEntity();
        RoundEntity roundEntity = getRoundEntity(questionEntity);
        PlayerEntity playerEntity = getPlayerEntity();
        RoomEntity roomEntity = getRoomEntity();
        AnswerEntity answer = getAnswerEntity();
        setupRepositoryStubs(roomEntity, playerEntity, roundEntity, gameSession, answer);

        // act
        SortAnswersCommand sortAnswersCommand =
                new SortAnswersCommand(ROOM_CODE, ROUND_ID, HOST_PLAYER_ID, ANSWER_ID);
        sortAnswerService.addRanking(sortAnswersCommand);

        // assert
        ArgumentCaptor<RankingEntity> rankingCaptor = ArgumentCaptor.forClass(RankingEntity.class);
        verify(rankingRepository, Mockito.times(1)).save(rankingCaptor.capture());
        RankingEntity addedRanking = rankingCaptor.getValue();
        assertThat(addedRanking.getRoundId()).isEqualTo(ROUND_ID);
        assertThat(addedRanking.getAnswer()).isEqualTo(answer);
        assertThat(addedRanking.getPosition()).isEqualTo(1);
    }

    private void setupRepositoryStubs(RoomEntity roomEntity, PlayerEntity playerEntity, RoundEntity roundEntity, GameSession gameSession, AnswerEntity answer) {
        when(roomCodeService.normalizeRoomCode(any(SortAnswersCommand.class))).thenReturn(ROOM_CODE);
        when(roomRepository.findByCode(anyString())).thenReturn(Optional.of(roomEntity));
        when(playerRepository.findById(any(UUID.class))).thenReturn(Optional.of(playerEntity));
        when(roundRepository.findById(any(UUID.class))).thenReturn(Optional.of(roundEntity));
        when(gameSessionRepository.findByRoomId(any(UUID.class))).thenReturn(Optional.of(gameSession));
        when(jpaAnswerRepository.findById(any(UUID.class))).thenReturn(Optional.of(answer));
        when(rankingRepository.findByRoundIdAndAnswer(ROUND_ID, answer)).thenReturn(Optional.empty());
    }

    @Test
    void getOrderOfAnswers() {
    }
}
