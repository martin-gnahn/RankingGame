package com.example.rankinggame.mapper;

import com.example.rankinggame.engine.Game;
import com.example.rankinggame.engine.GameId;
import com.example.rankinggame.engine.Player;
import com.example.rankinggame.engine.PlayerId;
import com.example.rankinggame.engine.QuestionId;
import com.example.rankinggame.engine.Round;
import com.example.rankinggame.entities.GameSession;
import com.example.rankinggame.entities.GameSessionStatus;
import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.entities.RoundEntity;
import com.example.rankinggame.entities.RoundState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MapperTest {
    private final PlayerMapper playerMapper = new PlayerMapper();
    private final RoundMapper roundMapper = new RoundMapper();
    private final GameMapper gameMapper = new GameMapper(playerMapper, roundMapper);

    @Test
    void playerMapperMapsBothDirections() {
        UUID playerId = UUID.randomUUID();
        PlayerEntity entity = new PlayerEntity();
        entity.setId(playerId);
        entity.setNickname("Marta");

        Player domain = playerMapper.toDomain(entity);
        PlayerEntity mappedEntity = playerMapper.toEntity(domain);

        assertThat(domain.playerId()).isEqualTo(new PlayerId(playerId));
        assertThat(domain.playerName()).isEqualTo("Marta");
        assertThat(mappedEntity.getId()).isEqualTo(playerId);
        assertThat(mappedEntity.getNickname()).isEqualTo("Marta");
    }

    @Test
    void roundMapperMapsBothDirections() {
        UUID questionId = UUID.randomUUID();
        UUID captainPlayerId = UUID.randomUUID();
        RoundEntity entity = new RoundEntity();
        entity.setQuestionId(questionId);
        entity.setCaptainPlayerId(captainPlayerId);
        entity.setState(RoundState.QUESTION_REVEALED);

        Round domain = roundMapper.toDomain(entity);
        RoundEntity mappedEntity = roundMapper.toEntity(domain);

        assertThat(domain.getQuestionId()).isEqualTo(new QuestionId(questionId));
        assertThat(domain.getCaptainPlayerId()).isEqualTo(new PlayerId(captainPlayerId));
        assertThat(domain.getRoundState()).isEqualTo(RoundState.QUESTION_REVEALED);
        assertThat(mappedEntity.getQuestionId()).isEqualTo(questionId);
        assertThat(mappedEntity.getCaptainPlayerId()).isEqualTo(captainPlayerId);
        assertThat(mappedEntity.getState()).isEqualTo(RoundState.QUESTION_REVEALED);
    }

    @Test
    void gameMapperMapsBothDirections() {
        UUID gameId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID captainPlayerId = UUID.randomUUID();

        Game domain = Game.builder()
                .gameId(new GameId(gameId))
                .players(List.of(new Player(new PlayerId(playerId), "Alex")))
                .allRounds(List.of(Round.builder()
                        .questionId(new QuestionId(questionId))
                        .captainPlayerId(new PlayerId(captainPlayerId))
                        .roundState(RoundState.ANSWER_SUBMISSION)
                        .build()))
                .status(GameSessionStatus.IN_PROGRESS)
                .currentRoundNumber(1)
                .build();

        GameSession entity = gameMapper.toEntity(domain);
        Game mappedDomain = gameMapper.toDomain(entity);

        assertThat(entity.getId()).isEqualTo(gameId);
        assertThat(entity.getStatus()).isEqualTo(GameSessionStatus.IN_PROGRESS);
        assertThat(entity.getCurrentRoundNumber()).isEqualTo(1);
        assertThat(entity.getPlayers())
                .singleElement()
                .satisfies(player -> {
                    assertThat(player.getId()).isEqualTo(playerId);
                    assertThat(player.getNickname()).isEqualTo("Alex");
                });
        assertThat(entity.getRounds())
                .singleElement()
                .satisfies(round -> {
                    assertThat(round.getQuestionId()).isEqualTo(questionId);
                    assertThat(round.getCaptainPlayerId()).isEqualTo(captainPlayerId);
                    assertThat(round.getState()).isEqualTo(RoundState.ANSWER_SUBMISSION);
                });
        assertThat(mappedDomain.getGameId()).isEqualTo(new GameId(gameId));
        assertThat(mappedDomain.getPlayers()).singleElement().isEqualTo(new Player(new PlayerId(playerId), "Alex"));
        assertThat(mappedDomain.getAllRounds()).singleElement().satisfies(round -> {
            assertThat(round.getQuestionId()).isEqualTo(new QuestionId(questionId));
            assertThat(round.getCaptainPlayerId()).isEqualTo(new PlayerId(captainPlayerId));
            assertThat(round.getRoundState()).isEqualTo(RoundState.ANSWER_SUBMISSION);
        });
    }
}
