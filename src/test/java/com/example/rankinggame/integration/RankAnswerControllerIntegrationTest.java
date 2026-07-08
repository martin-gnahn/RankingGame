package com.example.rankinggame.integration;

import com.example.rankinggame.entities.AnswerEntity;
import com.example.rankinggame.entities.RankedAnswerEntity;
import com.example.rankinggame.entities.RoundEntity;
import com.example.rankinggame.entities.RoundState;
import com.example.rankinggame.repositories.JpaRankingRepository;
import com.example.rankinggame.repositories.RoundRepository;
import com.jayway.jsonpath.JsonPath;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RankAnswerControllerIntegrationTest extends BackendIntegrationTest {
    private static final String HOST_NAME = "Marta";
    private static final String GUEST_NAME = "Alex";
    private static final String REQUEST_MAPPING_SUB_PATH = "/api/rooms/{roomCode}/ranking-game/rounds/{roundId}/answer/position";
    private static final String ANSWER_POSITION_NEW = "%s/new".formatted(REQUEST_MAPPING_SUB_PATH);
    private static final String ALL_ANSWER_POSITIONS = "%s/all".formatted(REQUEST_MAPPING_SUB_PATH);
    private static final String ACCESS_DENIED = "ACCESS_DENIED";
    private static final String GAME_STATE_CONFLICT = "GAME_STATE_CONFLICT";
    private static final String HOST_ANSWER = "Host answer";
    private static final String GUEST_ANSWER = "Guest answer";

    @Autowired
    private JpaRankingRepository rankingRepository;
    @Autowired
    private RoundRepository roundRepository;

    @Test
    void hostCanRankSubmittedAnswerAndRankingIsPersisted() throws Exception {
        SortingRound round = prepareRoundInSortingState();

        callAndAssertRankAnswerRequest(round, round.hostPlayerId(), round.hostAnswerId(), 1);
        assertRoundState(round.roundId(), RoundState.SORTING);

        List<RankedAnswerEntity> allRankings = rankingRepository.findAll();
        assertThat(allRankings).hasSize(1);
        RankedAnswerEntity persistedRanking = allRankings.getFirst();
        assertThat(persistedRanking.getAnswer().getId()).isEqualTo(round.hostAnswerId());
        assertThat(persistedRanking.getPosition()).isEqualTo(1);
    }

    @Test
    void guestCannotRankSubmittedAnswer() throws Exception {
        SortingRound round = prepareRoundInSortingState();

        mockMvc.perform(post(ANSWER_POSITION_NEW,
                        round.roomCode(),
                        round.roundId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sortAnswerRequest(round.guestPlayerId(), round.hostAnswerId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ACCESS_DENIED))
                .andExpect(jsonPath("$.message").value("Only the host can sort submitted answers"));

        List<RankedAnswerEntity> allRankings = rankingRepository.findAll();
        assertThat(allRankings).isEmpty();
    }

    @Test
    void rankingMultipleAnswersAssignsConsecutivePositions() throws Exception {
        SortingRound round = prepareRoundInSortingState();

        callAndAssertRankAnswerRequest(round, round.hostPlayerId(), round.guestAnswerId(), 1);
        callAndAssertRankAnswerRequest(round, round.hostPlayerId(), round.hostAnswerId(), 2);
        assertRoundState(round.roundId(), RoundState.RESULT);

        // then query ranking_entries ORDER BY position.
        List<RankedAnswerEntity> allRankings = rankingRepository.findAll();
        assertThat(allRankings).hasSize(2)
                .extracting(RankedAnswerEntity::getAnswer)
                .extracting(AnswerEntity::getPlayerId, AnswerEntity::getText)
                .containsExactly(
                        Tuple.tuple(round.guestPlayerId(), GUEST_ANSWER),
                        Tuple.tuple(round.hostPlayerId(), HOST_ANSWER)
                );
    }

    @Test
    void rankingTheSameAnswerTwiceIsRejected() throws Exception {
        SortingRound round = prepareRoundInSortingState();

        callAndAssertRankAnswerRequest(round, round.hostPlayerId(), round.hostAnswerId(), 1);
        UUID answerId = round.hostAnswerId();
        mockMvc.perform(post(ANSWER_POSITION_NEW,
                        round.roomCode(),
                        round.roundId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sortAnswerRequest(round.hostPlayerId(), answerId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(GAME_STATE_CONFLICT))
                .andExpect(jsonPath("$.message").value("Answer already has been ranked"));
    }

    @Test
    void answerFromAnotherRoundCannotBeRanked() throws Exception {
        SortingRound thisRound = prepareRoundInSortingState();
        SortingRound otherRound = prepareRoundInSortingState();

        mockMvc.perform(post(ANSWER_POSITION_NEW,
                        thisRound.roomCode(),
                        thisRound.roundId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sortAnswerRequest(thisRound.hostPlayerId(), otherRound.hostAnswerId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(GAME_STATE_CONFLICT))
                .andExpect(jsonPath("$.message").value("Answer is not part of the requested round"));
    }

    @Disabled("Your exercise: start a game, submit only one answer, then try sorting before all players answered.")
    @Test
    void answersCannotBeRankedBeforeRoundIsInSortingState() throws Exception {
        CreatedRoom room = createRoom(HOST_NAME);
        joinRoom(room.roomCode(), GUEST_NAME);
        StartedRound startedRound = startRankingGame(room.roomCode(), room.hostPlayerId());

        UUID hostAnswerId = submitAnswer(room.roomCode(), startedRound.roundId(), room.hostPlayerId(), HOST_ANSWER);

        mockMvc.perform(post(ANSWER_POSITION_NEW,
                        room.roomCode(),
                        startedRound.roundId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sortAnswerRequest(room.hostPlayerId(), hostAnswerId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(GAME_STATE_CONFLICT))
                .andExpect(jsonPath("$.message").value("Answers can only be sorted in sorting mode"));
    }

    @Test
    void hostCanReadRankedAnswersBackInRankingOrder() throws Exception {
        SortingRound round = prepareRoundInSortingState();

        callAndAssertRankAnswerRequest(round, round.hostPlayerId(), round.guestAnswerId(), 1);
        callAndAssertRankAnswerRequest(round, round.hostPlayerId(), round.hostAnswerId(), 2);
        assertRoundState(round.roundId(), RoundState.RESULT);

        mockMvc.perform(get(ALL_ANSWER_POSITIONS,
                        round.roomCode(),
                        round.roundId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("playerId", String.valueOf(round.guestPlayerId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rankings[0].oneBasedPosition").value(1))
                .andExpect(jsonPath("$.rankings[1].oneBasedPosition").value(2))
                .andExpect(jsonPath("$.rankings[0].answerText").value(GUEST_ANSWER))
                .andExpect(jsonPath("$.rankings[1].answerText").value(HOST_ANSWER))
                .andExpect(jsonPath("$.rankings[0].playerId").value(round.guestPlayerId().toString()))
                .andExpect(jsonPath("$.rankings[1].playerId").value(round.hostPlayerId().toString()));
    }

    private SortingRound prepareRoundInSortingState() throws Exception {
        CreatedRoom room = createRoom(HOST_NAME);
        UUID guestPlayerId = joinRoom(room.roomCode(), GUEST_NAME);
        StartedRound startedRound = startRankingGame(room.roomCode(), room.hostPlayerId());

        UUID hostAnswerId = submitAnswer(room.roomCode(), startedRound.roundId(), room.hostPlayerId(), HOST_ANSWER);
        UUID guestAnswerId = submitAnswer(room.roomCode(), startedRound.roundId(), guestPlayerId, GUEST_ANSWER);

        assertRoundState(startedRound.roundId(), RoundState.SORTING);
        return new SortingRound(
                room.roomCode(),
                room.hostPlayerId(),
                guestPlayerId,
                startedRound.roundId(),
                hostAnswerId,
                guestAnswerId
        );
    }

    private CreatedRoom createRoom(String hostName) throws Exception {
        String responseBody = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerName":"%s"}
                                """.formatted(hostName)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomCode").isString())
                .andExpect(jsonPath("$.playerId").isString())
                .andExpect(jsonPath("$.host").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return new CreatedRoom(
                JsonPath.read(responseBody, "$.roomCode"),
                readUuid(responseBody, "$.playerId")
        );
    }

    private UUID joinRoom(String roomCode, String playerName) throws Exception {
        String responseBody = mockMvc.perform(post("/api/rooms/{roomCode}/players", roomCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerName":"%s"}
                                """.formatted(playerName)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomCode").value(roomCode))
                .andExpect(jsonPath("$.playerId").isString())
                .andExpect(jsonPath("$.host").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return readUuid(responseBody, "$.playerId");
    }

    private StartedRound startRankingGame(String roomCode, UUID hostPlayerId) throws Exception {
        String responseBody = mockMvc.perform(post("/api/rooms/{roomCode}/ranking-game/start", roomCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerId":"%s"}
                                """.formatted(hostPlayerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roundId").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return new StartedRound(readUuid(responseBody, "$.roundId"));
    }

    private UUID submitAnswer(String roomCode, UUID roundId, UUID playerId, String answerText) throws Exception {
        String responseBody = mockMvc.perform(post("/api/rooms/{roomCode}/ranking-game/rounds/{roundId}/answers", roomCode, roundId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerId":"%s","answerText":"%s"}
                                """.formatted(playerId, answerText)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.answerId").isString())
                .andExpect(jsonPath("$.roundId").value(roundId.toString()))
                .andExpect(jsonPath("$.playerId").value(playerId.toString()))
                .andExpect(jsonPath("$.submitted").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return readUuid(responseBody, "$.answerId");
    }

    private void callAndAssertRankAnswerRequest(SortingRound round, UUID playerId, UUID answerId, int expectedPosition) throws Exception {
        mockMvc.perform(post(ANSWER_POSITION_NEW,
                        round.roomCode(),
                        round.roundId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sortAnswerRequest(playerId, answerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.answer.id").value(answerId.toString()))
                .andExpect(jsonPath("$.roundId").value(round.roundId().toString()))
                .andExpect(jsonPath("$.position").value(expectedPosition));
    }

    private String sortAnswerRequest(UUID hostPlayerId, UUID answerId) {
        return """
                {"hostId":"%s","answerId":"%s"}
                """.formatted(hostPlayerId, answerId);
    }

    private void assertRoundState(UUID roundId, RoundState expectedState) {
        Optional<RoundEntity> round = roundRepository.findById(roundId);
        assertThat(round).isPresent().get().extracting(RoundEntity::getState).isEqualTo(expectedState);
    }

    private UUID readUuid(String json, String path) {
        return UUID.fromString(JsonPath.read(json, path));
    }

    private record CreatedRoom(String roomCode, UUID hostPlayerId) {
    }

    private record StartedRound(UUID roundId) {
    }

    private record SortingRound(
            String roomCode,
            UUID hostPlayerId,
            UUID guestPlayerId,
            UUID roundId,
            UUID hostAnswerId,
            UUID guestAnswerId
    ) {
    }
}
