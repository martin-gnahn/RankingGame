package com.example.rankinggame.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RankAnswerControllerIntegrationTest extends BackendIntegrationTest {
    private static final String HOST_NAME = "Marta";
    private static final String GUEST_NAME = "Alex";

    @Test
    void hostCanRankSubmittedAnswerAndRankingIsPersisted() throws Exception {
        SortingRound round = prepareRoundInSortingState();

        mockMvc.perform(post("/api/rooms/{roomCode}/ranking-game/rounds/{roundId}/answer/position/new",
                        round.roomCode(),
                        round.roundId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sortAnswerRequest(round.hostPlayerId(), round.hostAnswerId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.answer.id").value(round.hostAnswerId().toString()))
                .andExpect(jsonPath("$.roundId").value(round.roundId().toString()))
                .andExpect(jsonPath("$.position").value(1));

        RankingEntry persistedRanking = findOnlyRankingEntry(round.roundId());
        assertThat(persistedRanking.answerId()).isEqualTo(round.hostAnswerId());
        assertThat(persistedRanking.position()).isEqualTo(1);
    }

    @Test
    void guestCannotRankSubmittedAnswer() throws Exception {
        SortingRound round = prepareRoundInSortingState();

        mockMvc.perform(post("/api/rooms/{roomCode}/ranking-game/rounds/{roundId}/answer/position/new",
                        round.roomCode(),
                        round.roundId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sortAnswerRequest(round.guestPlayerId(), round.hostAnswerId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Only the host can sort submitted answers"));

        Integer rankingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ranking_entries WHERE round_id = ?",
                Integer.class,
                round.roundId()
        );
        assertThat(rankingCount).isZero();
    }

    @Disabled("Your exercise: rank host answer, then guest answer, and assert positions 1 and 2 in the database.")
    @Test
    void rankingMultipleAnswersAssignsConsecutivePositions() {
        // Reuse prepareRoundInSortingState(), call the same POST twice with different answer ids,
        // then query ranking_entries ORDER BY position.
    }

    @Disabled("Your exercise: rank one answer once, repeat the same request, expect 409 GAME_STATE_CONFLICT.")
    @Test
    void rankingTheSameAnswerTwiceIsRejected() {
        // After the second request, also assert that only one row exists for that answer.
    }

    @Disabled("Your exercise: create two rooms/rounds, then try to rank answer A through round B's URL.")
    @Test
    void answerFromAnotherRoundCannotBeRanked() {
        // This protects the important URL/body consistency rule.
    }

    @Disabled("Your exercise: start a game, submit only one answer, then try sorting before all players answered.")
    @Test
    void answersCannotBeRankedBeforeRoundIsInSortingState() {
        // Expected status is 409 because the round is still ANSWER_SUBMISSION.
    }

    @Disabled("Future controller work: /position/all currently returns List.of(), so this should drive that endpoint.")
    @Test
    void hostCanReadRankedAnswersBackInRankingOrder() {
        // First implement the read use case, then assert answer ids in exact ranking order.
    }

    private SortingRound prepareRoundInSortingState() throws Exception {
        CreatedRoom room = createRoom(HOST_NAME);
        UUID guestPlayerId = joinRoom(room.roomCode(), GUEST_NAME);
        StartedRound startedRound = startRankingGame(room.roomCode(), room.hostPlayerId());

        UUID hostAnswerId = submitAnswer(room.roomCode(), startedRound.roundId(), room.hostPlayerId(), "Host answer");
        UUID guestAnswerId = submitAnswer(room.roomCode(), startedRound.roundId(), guestPlayerId, "Guest answer");

        assertRoundState(startedRound.roundId(), "SORTING");
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

    private String sortAnswerRequest(UUID hostPlayerId, UUID answerId) {
        return """
                {"hostId":"%s","answerId":"%s"}
                """.formatted(hostPlayerId, answerId);
    }

    private void assertRoundState(UUID roundId, String expectedState) {
        String actualState = jdbcTemplate.queryForObject(
                "SELECT state FROM rounds WHERE id = ?",
                String.class,
                roundId
        );
        assertThat(actualState).isEqualTo(expectedState);
    }

    private RankingEntry findOnlyRankingEntry(UUID roundId) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT answer_id, position
                        FROM ranking_entries
                        WHERE round_id = ?
                        """,
                (rs, rowNum) -> new RankingEntry(
                        rs.getObject("answer_id", UUID.class),
                        rs.getInt("position")
                ),
                roundId
        );
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

    private record RankingEntry(UUID answerId, int position) {
    }
}
