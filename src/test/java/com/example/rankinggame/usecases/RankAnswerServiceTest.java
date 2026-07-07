package com.example.rankinggame.usecases;

import com.example.rankinggame.controllers.GetRankingPositionsCommand;
import com.example.rankinggame.dto.AddRankingPositionCommand;
import com.example.rankinggame.engine.AnswerId;
import com.example.rankinggame.engine.RankedAnswer;
import com.example.rankinggame.entities.*;
import com.example.rankinggame.events.AnswerRankedEvent;
import com.example.rankinggame.mapper.*;
import com.example.rankinggame.repositories.AnswerRepository;
import com.example.rankinggame.repositories.RankingRepository;
import com.example.rankinggame.repositories.RoundRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankAnswerServiceTest {

    private static final String ROOM_CODE = "ROOM";
    private static final UUID ROUND_ID = UUID.randomUUID();
    private static final UUID HOST_PLAYER_ID = UUID.randomUUID();
    private static final UUID GUEST_PLAYER_ID = UUID.randomUUID();
    private static final UUID ANSWER_ID = UUID.randomUUID();
    private static final UUID GAME_SESSION_ID = UUID.randomUUID();
    private static final String ANSWER_TEXT = "TestAnswer1";

    @Mock
    private RoundRepository roundRepository;
    @Mock
    private RankingRepository rankingRepository;
    @Mock
    private AnswerRepository answerRepository;
    @Mock
    private AnswerRankingContextLoader answerRankingContextLoader;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private RankAnswerService rankAnswerService;

    @BeforeEach
    void setUp() {
        rankAnswerService = service();
    }

    @Test
    void shouldAddRankingPositionIfAnswerNotRankedYetAndMoveRoundToResultAfterLastAnswer() {
        ArrangeTestParams params = new ArrangeTestParams(false, RoundState.SORTING, true, true);
        SortAnswerTestFixture fixture = arrangeEntitiesAndStubs(params);
        stubAddRankingContext(fixture);
        when(rankingRepository.saveAndFlush(any(RankedAnswerEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(roundRepository.findByIdForUpdate(ROUND_ID)).thenReturn(Optional.of(fixture.round()));
        when(rankingRepository.countByRoundId(ROUND_ID)).thenReturn(1);
        when(answerRepository.countByRoundId(ROUND_ID)).thenReturn(1);
        when(roundRepository.updateStateIfCurrent(ROUND_ID, RoundState.SORTING, RoundState.RESULT)).thenReturn(1);

        rankAnswerService.addRankingPosition(sortAnswersCommand(params));

        ArgumentCaptor<RankedAnswerEntity> rankingCaptor = ArgumentCaptor.forClass(RankedAnswerEntity.class);
        verify(rankingRepository).saveAndFlush(rankingCaptor.capture());
        RankedAnswerEntity addedRanking = rankingCaptor.getValue();
        assertThat(addedRanking.getRoundId()).isEqualTo(ROUND_ID);
        assertThat(addedRanking.getAnswer().getId()).isEqualTo(fixture.answer().getId());
        assertThat(addedRanking.getPosition()).isEqualTo(1);
        assertThat(fixture.round().getState()).isEqualTo(RoundState.RESULT);
        verify(roundRepository).findByIdForUpdate(ROUND_ID);
        verify(roundRepository).updateStateIfCurrent(ROUND_ID, RoundState.SORTING, RoundState.RESULT);
        verify(eventPublisher).publishEvent(new AnswerRankedEvent(ROUND_ID, new AnswerId(ANSWER_ID), 1));
    }

    @Test
    void shouldFailIfAnswerAlreadyRanked() {
        ArrangeTestParams params = new ArrangeTestParams(true, RoundState.SORTING, true, true);
        SortAnswerTestFixture fixture = arrangeEntitiesAndStubs(params);
        stubAddRankingContext(fixture);

        assertThatThrownBy(() -> rankAnswerService.addRankingPosition(sortAnswersCommand(params)))
                .isInstanceOf(AnswerAlreadyRankedException.class);
        verify(rankingRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldFailIfRoundIsNotInSortingState() {
        ArrangeTestParams params = new ArrangeTestParams(false, RoundState.ANSWER_SUBMISSION, true, true);
        SortAnswerTestFixture fixture = arrangeEntitiesAndStubs(params);
        stubAddRankingContext(fixture);

        assertThatThrownBy(() -> rankAnswerService.addRankingPosition(sortAnswersCommand(params)))
                .isInstanceOf(RoundNotInSortingStateException.class);
        verify(rankingRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldFailIfInvalidAnswerIsRanked() {
        ArrangeTestParams params = new ArrangeTestParams(false, RoundState.SORTING, false, true);
        SortAnswerTestFixture fixture = arrangeEntitiesAndStubs(params);
        stubAddRankingContext(fixture);

        assertThatThrownBy(() -> rankAnswerService.addRankingPosition(sortAnswersCommand(params)))
                .isInstanceOf(AnswerNotPartOfRequestedRoundException.class);
        verify(rankingRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldFailIfRequestingPlayerIsNotHost() {
        ArrangeTestParams params = new ArrangeTestParams(false, RoundState.SORTING, true, false);
        SortAnswerTestFixture fixture = arrangeEntitiesAndStubs(params);
        stubAddRankingContext(fixture);

        assertThatThrownBy(() -> rankAnswerService.addRankingPosition(sortAnswersCommand(params)))
                .isInstanceOf(OnlyHostCanSortAnswers.class);
        verify(rankingRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldReturnRankingPositionsAlsoInResultState() {
        ArrangeTestParams params = new ArrangeTestParams(true, RoundState.RESULT, true, true);
        SortAnswerTestFixture fixture = arrangeEntitiesAndStubs(params);
        when(answerRankingContextLoader.load(any(GetRankingPositionsCommand.class)))
                .thenReturn(new AnswerRankingContext(
                        fixture.round(),
                        Optional.empty(),
                        fixture.captainPlayer()
                ));

        List<RankedAnswer> rankingPositions = rankAnswerService.getRankingPositions(
                new GetRankingPositionsCommand(ROOM_CODE, ROUND_ID, HOST_PLAYER_ID)
        );

        assertThat(rankingPositions).hasSize(1);
        assertThat(rankingPositions.getFirst().getOneBasedPosition()).isEqualTo(1);
    }

    private SortAnswerTestFixture arrangeEntitiesAndStubs(ArrangeTestParams params) {
        AnswerEntity answer = getAnswerEntity();
        PlayerEntity captainPlayer = new PlayerEntity();
        captainPlayer.setId(HOST_PLAYER_ID);
        RoundEntity round = getRoundEntity(params.roundState());
        SortAnswerTestFixture fixture = new SortAnswerTestFixture(round, answer, captainPlayer);
        setupRepositoryStubs(fixture, params);
        return fixture;
    }

    private void setupRepositoryStubs(SortAnswerTestFixture fixture, ArrangeTestParams params) {
        if (params.answerExists()) {
            when(answerRepository.findByRoundIdOrderBySubmittedAtAsc(ROUND_ID))
                    .thenReturn(List.of(fixture.answer()));
        }
        if (params.alreadyRanked()) {
            RankedAnswerEntity rankedAnswerEntity = new RankedAnswerEntity(UUID.randomUUID(), fixture.answer(), ROUND_ID, 1);
            when(rankingRepository.findByRoundIdOrderByPositionAsc(ROUND_ID))
                    .thenReturn(List.of(rankedAnswerEntity));
        }
    }

    private void stubAddRankingContext(SortAnswerTestFixture fixture) {
        when(answerRankingContextLoader.load(any(AddRankingPositionCommand.class)))
                .thenReturn(new AnswerRankingContext(
                        fixture.round(),
                        Optional.of(fixture.answer()),
                        fixture.captainPlayer()
                ));
    }

    private AddRankingPositionCommand sortAnswersCommand(ArrangeTestParams params) {
        return new AddRankingPositionCommand(ROOM_CODE, ROUND_ID, requestingPlayerId(params), ANSWER_ID);
    }

    private UUID requestingPlayerId(ArrangeTestParams params) {
        return params.requestingPlayerIsCaptain() ? HOST_PLAYER_ID : GUEST_PLAYER_ID;
    }

    private AnswerEntity getAnswerEntity() {
        AnswerEntity answer = new AnswerEntity();
        answer.setId(ANSWER_ID);
        answer.setRoundId(ROUND_ID);
        answer.setPlayerId(GUEST_PLAYER_ID);
        answer.setText(ANSWER_TEXT);
        return answer;
    }

    private RoundEntity getRoundEntity(RoundState roundState) {
        return new RoundEntity(
                ROUND_ID,
                GAME_SESSION_ID,
                null,
                HOST_PLAYER_ID,
                roundState,
                LocalDateTime.now()
        );
    }

    private record ArrangeTestParams(
            boolean alreadyRanked,
            RoundState roundState,
            boolean answerExists,
            boolean requestingPlayerIsCaptain
    ) {
    }

    private RankAnswerService service() {
        AnswerMapper answerMapper = new AnswerMapper();
        RankingMapper rankingMapper = new RankingMapper(answerMapper);
        RoundMapper roundMapper = new RoundMapper(
                new QuestionMapper(),
                answerMapper,
                rankingMapper,
                new PlayerMapper()
        );
        RoundProgressService roundProgressService = new RoundProgressService(
                roundRepository,
                mock(GameParticipantContextLoader.class),
                roundMapper,
                answerRepository,
                rankingRepository
        );

        return new RankAnswerService(
                rankingRepository,
                answerRepository,
                roundMapper,
                answerMapper,
                rankingMapper,
                answerRankingContextLoader,
                eventPublisher,
                roundProgressService
        );
    }

    private record SortAnswerTestFixture(
            RoundEntity round,
            AnswerEntity answer,
            PlayerEntity captainPlayer
    ) {
    }
}
