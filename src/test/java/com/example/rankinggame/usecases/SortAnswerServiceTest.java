package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.SortAnswerCommand;
import com.example.rankinggame.entities.*;
import com.example.rankinggame.mapper.*;
import com.example.rankinggame.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SortAnswerServiceTest {

    private static final String ROOM_CODE = "ROOM";
    private static final UUID ROOM_ID = UUID.randomUUID();
    private static final UUID ROUND_ID = UUID.randomUUID();
    private static final UUID HOST_PLAYER_ID = UUID.randomUUID();
    private static final UUID GUEST_PLAYER_ID = UUID.randomUUID();
    private static final UUID ANSWER_ID = UUID.randomUUID();
    private static final UUID GAME_SESSION_ID = UUID.randomUUID();
    private static final String ANSWER_TEXT = "TestAnswer1";
    private static final String EMPTY_ANSWER = " ";

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
    private RankingRepository rankingRepository;
    @Mock
    private AnswerRepository jpaAnswerRepository;

    @InjectMocks
    private AnswerRankingContextLoader answerRankingContextLoader;

    @BeforeEach
    void setUp() {
        sortAnswerService = service();
    }

    private GameSession getGameSessionEntity() {
        GameSession gameSession = new GameSession();
        gameSession.setId(GAME_SESSION_ID);
        gameSession.setCurrentRoundId(ROUND_ID);
        gameSession.setRoomId(ROOM_ID);
        return gameSession;
    }

    private RoundEntity getRoundEntity(QuestionEntity questionEntity, RoundState roundState) {
        return new RoundEntity(
                ROUND_ID, GAME_SESSION_ID, questionEntity,
                HOST_PLAYER_ID, roundState, LocalDateTime.now()
        );
    }

    private AnswerEntity getAnswerEntity() {
        AnswerEntity answer = new AnswerEntity();
        answer.setId(ANSWER_ID);
        answer.setRoundId(ROUND_ID);
        answer.setText(ANSWER_TEXT);
        answer.setCardValue(10);
        return answer;
    }

    private RoomEntity getRoomEntity() {
        return new RoomEntity(ROOM_ID, ROOM_CODE, HOST_PLAYER_ID, RoomStatus.IN_GAME, Instant.now());
    }

    private PlayerEntity getPlayerEntity(ArrangeTestParams params) {
        // TODO: currently some duplicate ids here (player has room id and room has host player id: redundancy)
        PlayerEntity playerEntity = new PlayerEntity();
        playerEntity.setId(requestingPlayerId(params));
        playerEntity.setHost(params.requestingPlayerIsCaptain());
        playerEntity.setRoomId(ROOM_ID);
        return playerEntity;
    }

    // TODO: add new failing test for empty answers, and failing test for card value outside 1 to 10

    @Test
    void shouldAddRankingIfAnswerNotRankedYet() {
        // arrange
        ArrangeTestParams params = new ArrangeTestParams(false, RoundState.SORTING, true, true);
        SortAnswerTestFixture fixture = arrangeEntitiesAndStubs(params);

        // act
        SortAnswerCommand sortAnswerCommand = sortAnswersCommand(params);
        sortAnswerService.addRanking(sortAnswerCommand);

        // assert
        ArgumentCaptor<RankingEntity> rankingCaptor = ArgumentCaptor.forClass(RankingEntity.class);
        verify(rankingRepository).save(rankingCaptor.capture());
        RankingEntity addedRanking = rankingCaptor.getValue();
        assertThat(addedRanking.getRoundId()).isEqualTo(ROUND_ID);
        assertThat(addedRanking.getAnswer()).isEqualTo(fixture.answer());
        assertThat(addedRanking.getPosition()).isEqualTo(1);
    }

    @Test
    void shouldFailIfAnswerAlreadyRanked() {
        // arrange
        ArrangeTestParams params = new ArrangeTestParams(true, RoundState.SORTING, true, true);
        arrangeEntitiesAndStubs(params);

        // act
        SortAnswerCommand sortAnswerCommand = sortAnswersCommand(params);

        // assert
        assertThatThrownBy(() -> sortAnswerService.addRanking(sortAnswerCommand))
                .isInstanceOf(AnswerAlreadyRankedException.class);
        verify(rankingRepository, never()).save(any());
    }

    @Test
    void shouldFailIfRoundIsNotInSortingState() {
        // arrange
        ArrangeTestParams params = new ArrangeTestParams(false, RoundState.ANSWER_SUBMISSION, true, true);
        arrangeEntitiesAndStubs(params);

        // act
        SortAnswerCommand sortAnswerCommand = sortAnswersCommand(params);

        // assert
        assertThatThrownBy(() -> sortAnswerService.addRanking(sortAnswerCommand))
                .isInstanceOf(RoundNotInSortingStateException.class);
        verify(rankingRepository, never()).save(any());
    }

    @Test
    void shouldFailIfInvalidAnswerIsRanked() {
        // arrange
        ArrangeTestParams params = new ArrangeTestParams(false, RoundState.SORTING, false, true);
        arrangeEntitiesAndStubs(params);

        // act
        SortAnswerCommand sortAnswerCommand = sortAnswersCommand(params);

        // assert
        assertThatThrownBy(() -> sortAnswerService.addRanking(sortAnswerCommand))
                .isInstanceOf(AnswerNotPartOfRequestedRoundException.class);
        verify(rankingRepository, never()).save(any());
    }

    @Test
    void shouldFailIfRequestingPlayerIsNotHost() {
        // arrange
        ArrangeTestParams params = new ArrangeTestParams(false, RoundState.SORTING, true, false);
        arrangeEntitiesAndStubs(params);

        // act
        SortAnswerCommand sortAnswerCommand = sortAnswersCommand(params);

        // assert
        assertThatThrownBy(() -> sortAnswerService.addRanking(sortAnswerCommand))
                .isInstanceOf(OnlyHostCanSortAnswers.class);
        verify(rankingRepository, never()).save(any());
    }

    private SortAnswerTestFixture arrangeEntitiesAndStubs(ArrangeTestParams params) {
        GameSession gameSession = getGameSessionEntity();
        QuestionEntity questionEntity = new QuestionEntity();
        RoundEntity roundEntity = getRoundEntity(questionEntity, params.roundState());
        PlayerEntity playerEntity = getPlayerEntity(params);
        RoomEntity roomEntity = getRoomEntity();
        AnswerEntity answer = getAnswerEntity();
        SortAnswerTestFixture fixture = new SortAnswerTestFixture(roomEntity, playerEntity, roundEntity, gameSession, answer);
        setupRepositoryStubs(fixture, params);
        return fixture;
    }

    private void setupRepositoryStubs(SortAnswerTestFixture fixture, ArrangeTestParams params) {
        when(roomCodeService.normalizeRoomCode(any(SortAnswerCommand.class))).thenReturn(ROOM_CODE);
        when(roomRepository.findByCode(ROOM_CODE)).thenReturn(Optional.of(fixture.room()));
        when(playerRepository.findById(requestingPlayerId(params))).thenReturn(Optional.of(fixture.player()));
        if (!params.requestingPlayerIsCaptain()) {
            return;
        }
        when(roundRepository.findById(ROUND_ID)).thenReturn(Optional.of(fixture.round()));
        when(gameSessionRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.of(fixture.gameSession()));
        when(jpaAnswerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(fixture.answer()));
        if (params.answerExists()) {
            when(jpaAnswerRepository.findByRoundIdOrderBySubmittedAtAsc(ROUND_ID))
                    .thenReturn(List.of(fixture.answer()));
        }
        if (params.alreadyRanked()) {
            RankingEntity rankingEntity = new RankingEntity(UUID.randomUUID(), fixture.answer(), ROUND_ID, 1);
            when(rankingRepository.findByRoundIdOrderByPositionAsc(ROUND_ID))
                    .thenReturn(List.of(rankingEntity));
        }
    }

    private SortAnswerCommand sortAnswersCommand(ArrangeTestParams params) {
        return new SortAnswerCommand(ROOM_CODE, ROUND_ID, requestingPlayerId(params), ANSWER_ID);
    }

    private UUID requestingPlayerId(ArrangeTestParams params) {
        return params.requestingPlayerIsCaptain() ? HOST_PLAYER_ID : GUEST_PLAYER_ID;
    }

    private record ArrangeTestParams(
            boolean alreadyRanked,
            RoundState roundState,
            boolean answerExists,
            boolean requestingPlayerIsCaptain
    ) {
    }

    private record SortAnswerTestFixture(
            RoomEntity room,
            PlayerEntity player,
            RoundEntity round,
            GameSession gameSession,
            AnswerEntity answer
    ) {
    }

    private SortAnswerService service() {
        AnswerMapper answerMapper = new AnswerMapper();
        QuestionMapper questionMapper = new QuestionMapper();
        RankingMapper rankingMapper = new RankingMapper(answerMapper);
        RoundMapper roundMapper = new RoundMapper(questionMapper, answerMapper, rankingMapper, new PlayerMapper());

        return new SortAnswerService(
                roomCodeService,
                roomRepository,
                playerRepository,
                roundRepository,
                gameSessionRepository,
                rankingRepository,
                jpaAnswerRepository,
                roundMapper,
                answerMapper,
                rankingMapper,
                answerRankingContextLoader
        );
    }
}
