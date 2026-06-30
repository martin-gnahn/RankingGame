package com.example.rankinggame.integration;

import com.example.rankinggame.dto.StartGameResponse;
import com.example.rankinggame.dto.SubmitAnswerResponse;
import com.example.rankinggame.dto.SubmittedAnswerResponse;
import com.example.rankinggame.dto.SubmittedAnswersResponse;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoomFlowIntegrationTest extends BackendIntegrationTest {
    public static final String MARTA = "Marta";
    public static final String ALEX = "Alex";
    public static final String SAM = "Sam";

    @Test
    void createsRoomJoinsSecondPlayerAndReadsLobbyFromPostgres() throws Exception {
        String createRoomResponse = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerName\":\"" + MARTA + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomCode").isString())
                .andExpect(jsonPath("$.roomId").isString())
                .andExpect(jsonPath("$.playerId").isString())
                .andExpect(jsonPath("$.nickname").value(MARTA))
                .andExpect(jsonPath("$.host").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String roomCode = JsonPath.read(createRoomResponse, "$.roomCode");

        mockMvc.perform(post("/api/rooms/{roomCode}/players", roomCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerName\":\"" + ALEX + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomCode").value(roomCode))
                .andExpect(jsonPath("$.nickname").value(ALEX))
                .andExpect(jsonPath("$.host").value(false));

        mockMvc.perform(get("/api/rooms/{roomCode}", roomCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomCode").value(roomCode))
                .andExpect(jsonPath("$.status").value("LOBBY"))
                .andExpect(jsonPath("$.players", hasSize(2)))
                .andExpect(jsonPath("$.players[0].nickname").value(MARTA))
                .andExpect(jsonPath("$.players[0].host").value(true))
                .andExpect(jsonPath("$.players[0].connectionStatus").value("CONNECTED"))
                .andExpect(jsonPath("$.players[1].nickname").value(ALEX))
                .andExpect(jsonPath("$.players[1].host").value(false))
                .andExpect(jsonPath("$.players[1].connectionStatus").value("CONNECTED"));
    }

    @Test
    void hostCanQueryBothSubmittedAnswersAfterAllPlayersAnswered() throws Exception {
        String createRoomResponse = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerName\":\"" + MARTA + "\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String roomCode = JsonPath.read(createRoomResponse, "$.roomCode");
        String hostPlayerId = JsonPath.read(createRoomResponse, "$.playerId");

        String joinRoomResponse = mockMvc.perform(post("/api/rooms/{roomCode}/players", roomCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerName\":\"" + ALEX + "\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String guestPlayerId = JsonPath.read(joinRoomResponse, "$.playerId");

        String startGameResponse = mockMvc.perform(post("/api/rooms/{roomCode}/ranking-game/start", roomCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hostPlayerId\":\"" + hostPlayerId + "\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String roundId = JsonPath.read(startGameResponse, "$.roundId");

        mockMvc.perform(post("/api/rooms/{roomCode}/ranking-game/rounds/{roundId}/answers", roomCode, roundId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "playerId": "%s",
                                  "value": "Answer1"
                                }
                                """.formatted(hostPlayerId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/rooms/{roomCode}/ranking-game/rounds/{roundId}/answers", roomCode, roundId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "playerId": "%s",
                                  "value": "Answer2"
                                }
                                """.formatted(guestPlayerId)))
                .andExpect(status().isCreated());

        String roundState = jdbcTemplate.queryForObject(
                "SELECT state FROM rounds WHERE id = ?",
                String.class,
                UUID.fromString(roundId)
        );
        org.assertj.core.api.Assertions.assertThat(roundState).isEqualTo("SORTING");

        mockMvc.perform(get("/api/rooms/{roomCode}/ranking-game/rounds/{roundId}/answers", roomCode, roundId)
                        .param("playerId", hostPlayerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answers", hasSize(2)))
                .andExpect(jsonPath("$.answers[*].answerText", containsInAnyOrder("Answer1", "Answer2")))
                .andExpect(jsonPath("$.answers[*].playerId", containsInAnyOrder(hostPlayerId, guestPlayerId)));

        mockMvc.perform(get("/api/rooms/{roomCode}/ranking-game/rounds/{roundId}/answers", roomCode, roundId)
                        .param("playerId", guestPlayerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answers", hasSize(2)))
                .andExpect(jsonPath("$.answers[*].answerText", containsInAnyOrder("Answer1", "Answer2")))
                .andExpect(jsonPath("$.answers[*].playerId", containsInAnyOrder(hostPlayerId, guestPlayerId)));
    }

    @Test
    void preparesThreePlayerGameForAnswerSubmission() throws Exception {
        // Given: one host creates a room.
        CreatedRoom createdRoom = createRoom(MARTA);

        String firstGuestPlayerId = joinRoom(createdRoom, ALEX);
        String secondGuestPlayerId = joinRoom(createdRoom, SAM);

        // Then: the lobby contains all three connected players, and the game has not started yet.
        ensurePlayersConnected(createdRoom, firstGuestPlayerId, secondGuestPlayerId);

        StartGameResponse startedGame = startGame(createdRoom);

        ensurePlayersConnectedInGame(createdRoom, firstGuestPlayerId, secondGuestPlayerId);

        SubmittedAnswers submittedAnswers = submitAnswersForAllPlayers(
                createdRoom,
                startedGame,
                firstGuestPlayerId,
                secondGuestPlayerId
        );
        CurrentGameSessionState currentGameSessionState = queryCurrentGameSessionState(startedGame);

        assertThat(submittedAnswers.hostAnswer().submitted()).isTrue();
        assertThat(submittedAnswers.firstGuestAnswer().submitted()).isTrue();
        assertThat(submittedAnswers.secondGuestAnswer().submitted()).isTrue();
        assertThat(currentGameSessionState.roomStatus()).isEqualTo("IN_GAME");
        assertThat(currentGameSessionState.gameType()).isEqualTo("RANKING_GAME");
        assertThat(currentGameSessionState.gameSessionStatus()).isEqualTo("IN_PROGRESS");
        assertThat(currentGameSessionState.currentRoundIndex()).isZero();
        assertThat(currentGameSessionState.roundState()).isEqualTo("SORTING");
        assertThat(currentGameSessionState.answerCount()).isEqualTo(3);
        assertThat(currentGameSessionState.distinctAnswerPlayerCount()).isEqualTo(3);

        QueriedSubmittedAnswers queriedSubmittedAnswers = querySubmittedAnswersForAllPlayers(
                createdRoom,
                startedGame,
                firstGuestPlayerId,
                secondGuestPlayerId
        );
        queriedSubmittedAnswers.assertAllPlayersSeeSameAnswers();
        sortSubmittedAnswersAsHost(createdRoom, startedGame);
        rejectSortSubmittedAnswersAsGuest(createdRoom, startedGame, firstGuestPlayerId);

        int a = 0;

        // Future backbone: submit a real ranking payload and persist ranking entries.
    }

    private QueriedSubmittedAnswers querySubmittedAnswersForAllPlayers(
            CreatedRoom createdRoom,
            StartGameResponse startedGame,
            String firstGuestPlayerId,
            String secondGuestPlayerId
    ) throws Exception {
        return new QueriedSubmittedAnswers(
                querySubmittedAnswers(createdRoom, startedGame, createdRoom.hostPlayerId(), firstGuestPlayerId, secondGuestPlayerId),
                querySubmittedAnswers(createdRoom, startedGame, firstGuestPlayerId, firstGuestPlayerId, secondGuestPlayerId),
                querySubmittedAnswers(createdRoom, startedGame, secondGuestPlayerId, firstGuestPlayerId, secondGuestPlayerId)
        );
    }

    private SubmittedAnswersResponse querySubmittedAnswers(
            CreatedRoom createdRoom,
            StartGameResponse startedGame,
            String requestingPlayerId,
            String firstGuestPlayerId,
            String secondGuestPlayerId
    ) throws Exception {
        String responseBody = mockMvc.perform(get("/api/rooms/{roomCode}/ranking-game/rounds/{roundId}/answers",
                            createdRoom.roomCode(),
                            startedGame.roundId()
                        )
                        .param("playerId", requestingPlayerId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        SubmittedAnswersResponse response = readSubmittedAnswersResponse(responseBody);
        assertThat(response.answers()).hasSize(3);
        assertThat(response.answers())
                .extracting(SubmittedAnswerResponse::answerText)
                .containsExactlyInAnyOrder("Answer1", "Answer2", "Answer3");
        assertThat(response.answers())
                .extracting(answer -> answer.playerId().toString())
                .containsExactlyInAnyOrder(createdRoom.hostPlayerId(), firstGuestPlayerId, secondGuestPlayerId);
        return response;
    }

    private void sortSubmittedAnswersAsHost(
            CreatedRoom createdRoom,
            StartGameResponse startedGame
    ) throws Exception {
        mockMvc.perform(post("/api/rooms/{roomCode}/ranking-game/rounds/{roundId}/sort",
                            createdRoom.roomCode(),
                            startedGame.roundId()
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hostPlayerId\":\"" + createdRoom.hostPlayerId() + "\"}"))
                .andExpect(status().isNoContent());
    }

    private void rejectSortSubmittedAnswersAsGuest(
            CreatedRoom createdRoom,
            StartGameResponse startedGame,
            String guestPlayerId
    ) throws Exception {
        mockMvc.perform(post("/api/rooms/{roomCode}/ranking-game/rounds/{roundId}/sort",
                            createdRoom.roomCode(),
                            startedGame.roundId()
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hostPlayerId\":\"" + guestPlayerId + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Only the host can sort submitted answers"));
    }

    private SubmittedAnswers submitAnswersForAllPlayers(
            CreatedRoom createdRoom,
            StartGameResponse startedGame,
            String firstGuestPlayerId,
            String secondGuestPlayerId
    ) throws Exception {
        SubmitAnswerResponse hostAnswer = submitAnswer(
                createdRoom,
                startedGame,
                createdRoom.hostPlayerId(),
                "Answer1"
        );
        SubmitAnswerResponse firstGuestAnswer = submitAnswer(
                createdRoom,
                startedGame,
                firstGuestPlayerId,
                "Answer2"
        );
        SubmitAnswerResponse secondGuestAnswer = submitAnswer(
                createdRoom,
                startedGame,
                secondGuestPlayerId,
                "Answer3"
        );

        return new SubmittedAnswers(hostAnswer, firstGuestAnswer, secondGuestAnswer);
    }

    private SubmitAnswerResponse submitAnswer(
            CreatedRoom createdRoom,
            StartGameResponse startedGame,
            String playerId,
            String answerText
    ) throws Exception {
        String submitAnswerResponse = mockMvc.perform(post(
                            "/api/rooms/{roomCode}/ranking-game/rounds/{roundId}/answers",
                            createdRoom.roomCode(),
                            startedGame.roundId()
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "playerId": "%s",
                                  "value": "%s"
                                }
                                """.formatted(playerId, answerText))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.answerId").isString())
                .andExpect(jsonPath("$.roundId").value(startedGame.roundId().toString()))
                .andExpect(jsonPath("$.playerId").value(playerId))
                .andExpect(jsonPath("$.submitted").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return new SubmitAnswerResponse(
                readUuid(submitAnswerResponse, "$.answerId"),
                readUuid(submitAnswerResponse, "$.roundId"),
                readUuid(submitAnswerResponse, "$.playerId"),
                JsonPath.read(submitAnswerResponse, "$.submitted")
        );
    }

    private CurrentGameSessionState queryCurrentGameSessionState(StartGameResponse startedGame) {
        return jdbcTemplate.queryForObject("""
                        SELECT
                            rooms.status AS room_status,
                            game_sessions.game_type AS game_type,
                            game_sessions.status AS game_session_status,
                            game_sessions.current_round_index AS current_round_index,
                            rounds.state AS round_state,
                            COUNT(answers.id)::int AS answer_count,
                            COUNT(DISTINCT answers.player_id)::int AS distinct_answer_player_count
                        FROM game_sessions
                        JOIN rooms ON rooms.id = game_sessions.room_id
                        JOIN rounds ON rounds.game_session_id = game_sessions.id
                        LEFT JOIN answers ON answers.round_id = rounds.id
                        WHERE game_sessions.id = ?
                          AND rounds.id = ?
                        GROUP BY
                            rooms.status,
                            game_sessions.game_type,
                            game_sessions.status,
                            game_sessions.current_round_index,
                            rounds.state
                        """,
                (rs, rowNum) -> new CurrentGameSessionState(
                        rs.getString("room_status"),
                        rs.getString("game_type"),
                        rs.getString("game_session_status"),
                        rs.getInt("current_round_index"),
                        rs.getString("round_state"),
                        rs.getInt("answer_count"),
                        rs.getInt("distinct_answer_player_count")
                ),
                startedGame.gameSessionId(),
                startedGame.roundId()
        );
    }

    private StartGameResponse startGame(CreatedRoom createdRoom) throws Exception {
        String startGameResponse = mockMvc.perform(post("/api/rooms/{roomCode}/ranking-game/start",
                            createdRoom.roomCode()
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hostPlayerId\":\"" + createdRoom.hostPlayerId() + "\"}")
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomCode").value(createdRoom.roomCode()))
                .andExpect(jsonPath("$.gameType").value("RANKING_GAME"))
                .andExpect(jsonPath("$.gameSessionId").isString())
                .andExpect(jsonPath("$.roundId").isString())
                .andExpect(jsonPath("$.roundNumber").value(1))
                .andExpect(jsonPath("$.questionId").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return new StartGameResponse(
                readUuid(startGameResponse, "$.roomId"),
                JsonPath.read(startGameResponse, "$.roomCode"),
                readUuid(startGameResponse, "$.gameSessionId"),
                JsonPath.read(startGameResponse, "$.gameType"),
                readUuid(startGameResponse, "$.roundId"),
                JsonPath.read(startGameResponse, "$.roundNumber"),
                readUuid(startGameResponse, "$.questionId")
        );
    }

    private void ensurePlayersConnectedInGame(
            CreatedRoom createdRoom,
            String firstGuestPlayerId,
            String secondGuestPlayerId
    ) throws Exception {
        mockMvc.perform(get("/api/rooms/{roomCode}", createdRoom.roomCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomCode").value(createdRoom.roomCode()))
                .andExpect(jsonPath("$.status").value("IN_GAME"))
                .andExpect(jsonPath("$.players", hasSize(3)))
                .andExpect(jsonPath("$.players[*].playerId", containsInAnyOrder(
                        createdRoom.hostPlayerId(),
                        firstGuestPlayerId,
                        secondGuestPlayerId
                )))
                .andExpect(jsonPath("$.players[*].nickname", containsInAnyOrder(MARTA, ALEX, SAM)))
                .andExpect(jsonPath("$.players[*].connectionStatus", containsInAnyOrder(
                        "CONNECTED",
                        "CONNECTED",
                        "CONNECTED"
                )));
    }

    private UUID readUuid(String json, String path) {
        return UUID.fromString(JsonPath.read(json, path));
    }

    private SubmittedAnswersResponse readSubmittedAnswersResponse(String json) {
        List<String> answerIds = JsonPath.read(json, "$.answers[*].answerId");
        List<String> playerIds = JsonPath.read(json, "$.answers[*].playerId");
        List<String> nicknames = JsonPath.read(json, "$.answers[*].nickname");
        List<String> answerTexts = JsonPath.read(json, "$.answers[*].answerText");
        List<Integer> cardValues = JsonPath.read(json, "$.answers[*].cardValue");

        List<SubmittedAnswerResponse> answers = new ArrayList<>();
        for (int i = 0; i < answerIds.size(); i++) {
            answers.add(new SubmittedAnswerResponse(
                    UUID.fromString(answerIds.get(i)),
                    UUID.fromString(playerIds.get(i)),
                    nicknames.get(i),
                    answerTexts.get(i),
                    cardValues.get(i)
            ));
        }

        return new SubmittedAnswersResponse(answers);
    }

    private void ensurePlayersConnected(CreatedRoom createdRoom, String firstGuestPlayerId, String secondGuestPlayerId) throws Exception {
        mockMvc.perform(get("/api/rooms/{roomCode}", createdRoom.roomCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomCode").value(createdRoom.roomCode()))
                .andExpect(jsonPath("$.status").value("LOBBY"))
                .andExpect(jsonPath("$.players", hasSize(3)))
                .andExpect(jsonPath("$.players[*].playerId", containsInAnyOrder(
                        createdRoom.hostPlayerId(),
                        firstGuestPlayerId,
                        secondGuestPlayerId
                )))
                .andExpect(jsonPath("$.players[*].nickname", containsInAnyOrder(MARTA, ALEX, SAM)))
                .andExpect(jsonPath("$.players[*].connectionStatus", containsInAnyOrder(
                        "CONNECTED",
                        "CONNECTED",
                        "CONNECTED"
                )));
    }

    private String joinRoom(CreatedRoom createdRoom, String guestName) throws Exception {
        String firstGuestResponse = mockMvc.perform(post("/api/rooms/{roomCode}/players", createdRoom.roomCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerName\":\"" + guestName + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomCode").value(createdRoom.roomCode()))
                .andExpect(jsonPath("$.playerId").isString())
                .andExpect(jsonPath("$.nickname").value(guestName))
                .andExpect(jsonPath("$.host").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(firstGuestResponse, "$.playerId");
    }

    private RoomFlowIntegrationTest.CreatedRoom createRoom(String hostName) throws Exception {
        String createRoomResponse = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerName\":\"%s\"}".formatted(hostName)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomCode").isString())
                .andExpect(jsonPath("$.playerId").isString())
                .andExpect(jsonPath("$.nickname").value(hostName))
                .andExpect(jsonPath("$.host").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String roomCode = JsonPath.read(createRoomResponse, "$.roomCode");
        String hostPlayerId = JsonPath.read(createRoomResponse, "$.playerId");
        return new CreatedRoom(roomCode, hostPlayerId);
    }

    private record CreatedRoom(String roomCode, String hostPlayerId) {
    }

    private record SubmittedAnswers(
            SubmitAnswerResponse hostAnswer,
            SubmitAnswerResponse firstGuestAnswer,
            SubmitAnswerResponse secondGuestAnswer
    ) {
    }

    private record QueriedSubmittedAnswers(
            SubmittedAnswersResponse hostAnswers,
            SubmittedAnswersResponse firstGuestAnswers,
            SubmittedAnswersResponse secondGuestAnswers
    ) {
        private void assertAllPlayersSeeSameAnswers() {
            List<ComparableSubmittedAnswer> hostView = comparableAnswers(hostAnswers);

            assertThat(comparableAnswers(firstGuestAnswers)).isEqualTo(hostView);
            assertThat(comparableAnswers(secondGuestAnswers)).isEqualTo(hostView);
        }

        private List<ComparableSubmittedAnswer> comparableAnswers(SubmittedAnswersResponse response) {
            return response.answers().stream()
                    .map(ComparableSubmittedAnswer::from)
                    .sorted(Comparator.comparing(ComparableSubmittedAnswer::answerId))
                    .toList();
        }
    }

    private record ComparableSubmittedAnswer(
            UUID answerId,
            UUID playerId,
            String nickname,
            String answerText,
            int cardValue
    ) {
        private static ComparableSubmittedAnswer from(SubmittedAnswerResponse answer) {
            return new ComparableSubmittedAnswer(
                    answer.answerId(),
                    answer.playerId(),
                    answer.nickname(),
                    answer.answerText(),
                    answer.cardValue()
            );
        }
    }

    private record CurrentGameSessionState(
            String roomStatus,
            String gameType,
            String gameSessionStatus,
            int currentRoundIndex,
            String roundState,
            int answerCount,
            int distinctAnswerPlayerCount
    ) {
    }
}
