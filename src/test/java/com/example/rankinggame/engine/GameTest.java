package com.example.rankinggame.engine;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GameTest {
    private static final String PLAYER1_NAME = "Player1";
    private static final String PLAYER2_NAME = "Player2";
    private static final UUID PLAYER1_ID = UUID.randomUUID();
    private static final UUID PLAYER2_ID = UUID.randomUUID();
    private static final String QUESTION_TEXT = "Question";
    private static final String QUESTION_CATEGORY = "default";

    @Test
    void canStartGame() {
        Game game = startGameWith2Players();

        assertThat(game.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
    }

    @Test
    void inactiveGameHasNoPoints() {
        GameParticipant player1 = new GameParticipant(new PlayerId(PLAYER1_ID), PLAYER1_NAME);
        GameParticipant player2 = new GameParticipant(new PlayerId(PLAYER2_ID), PLAYER2_NAME);
        List<GameParticipant> players = List.of(player1, player2);
        Game inactiveGame = new Game(players);

        assertThat(inactiveGame.getScore()).isEmpty();
    }

    @Test
    void activeGameHasCorrectStartingPoints() {
        Game game = startGameWith2Players();

        assertThat(game.getScore()).contains(GameConstants.DEFAULT_STARTING_POINTS);
    }

    private Game startGameWith2Players() {
        GameParticipant player1 = new GameParticipant(new PlayerId(PLAYER1_ID), PLAYER1_NAME);
        GameParticipant player2 = new GameParticipant(new PlayerId(PLAYER2_ID), PLAYER2_NAME);
        List<GameParticipant> players = List.of(player1, player2);
        Game game = new Game(players);
        Question firstQuestion = new Question(new QuestionId(UUID.randomUUID()), QUESTION_TEXT, QUESTION_CATEGORY);
        game.start(player1, firstQuestion);
        return game;
    }

}
