package com.example.rankinggame.mapper;

import com.example.rankinggame.engine.Game;
import com.example.rankinggame.engine.GameStatus;
import com.example.rankinggame.engine.GameParticipant;
import com.example.rankinggame.engine.PlayerId;
import com.example.rankinggame.engine.Question;
import com.example.rankinggame.engine.QuestionId;
import com.example.rankinggame.engine.Round;
import com.example.rankinggame.engine.RoundStatus;
import com.example.rankinggame.entities.GameSession;
import com.example.rankinggame.entities.GameSessionStatus;
import com.example.rankinggame.entities.GameType;
import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.entities.QuestionEntity;
import com.example.rankinggame.entities.RoundEntity;
import com.example.rankinggame.entities.RoundState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MapperTest {
    private final PlayerMapper playerMapper = new PlayerMapper();
    private final RoundMapper roundMapper = new RoundMapper(new QuestionMapper(), new AnswerMapper());
    private final GameMapper gameMapper = new GameMapper();

    @Test
    void playerMapperProjectsRoomPlayerToGameParticipant() {
        UUID playerId = UUID.randomUUID();
        PlayerEntity entity = new PlayerEntity();
        entity.setId(playerId);
        entity.setNickname("Marta");
        entity.setHost(true);

        GameParticipant domain = playerMapper.toParticipant(entity);

        assertThat(domain.playerId()).isEqualTo(new PlayerId(playerId));
        assertThat(domain.name()).isEqualTo("Marta");
        assertThat(domain.host()).isTrue();
    }

    @Test
    void roundMapperMapsEntityToDomainProjection() {
        UUID questionId = UUID.randomUUID();
        UUID captainPlayerId = UUID.randomUUID();
        RoundEntity entity = new RoundEntity();
        entity.setQuestionEntity(question(questionId));
        entity.setCaptainPlayerId(captainPlayerId);
        entity.setState(RoundState.ANSWER_SUBMISSION);

        Round domain = roundMapper.toDomain(entity, List.of());

        assertThat(domain.getRoundStatus()).isEqualTo(RoundStatus.ANSWER_SUBMISSION);
        assertThat(domain.getQuestion()).isEqualTo(new Question(new QuestionId(questionId), "Question", "test"));
        assertThat(domain.getCaptain()).isEqualTo(new GameParticipant(new PlayerId(captainPlayerId), null, false));
        assertThat(domain.getSubmittedAnswers()).isEmpty();
    }

    @Test
    void roundMapperMapsDomainToEntity() {
        UUID questionId = UUID.randomUUID();
        UUID captainPlayerId = UUID.randomUUID();
        Round domain = Round.builder()
                .roundStatus(RoundStatus.ANSWER_SUBMISSION)
                .captain(new GameParticipant(new PlayerId(captainPlayerId), "Alex", true))
                .question(new Question(new QuestionId(questionId), "Question", "test"))
                .build();

        RoundEntity entity = roundMapper.toEntity(domain);

        assertThat(entity.getState()).isEqualTo(RoundState.ANSWER_SUBMISSION);
        assertThat(entity.getCaptainPlayerId()).isEqualTo(captainPlayerId);
        assertThat(entity.getQuestionId()).isEqualTo(questionId);
        assertThat(entity.getQuestionEntity().getText()).isEqualTo("Question");
        assertThat(entity.getQuestionEntity().getCategory()).isEqualTo("test");
        assertThat(entity.getQuestionEntity().isActive()).isTrue();
    }

    @Test
    void gameMapperMapsDomainToGameSessionProjection() {
        Game domain = Game.builder()
                .status(GameStatus.IN_PROGRESS)
                .currentRoundIndex(2)
                .build();

        GameSession entity = gameMapper.toEntity(domain);

        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getStatus()).isEqualTo(GameSessionStatus.IN_PROGRESS);
        assertThat(entity.getCurrentRoundNumber()).isEqualTo(3);
        assertThat(entity.getGameType()).isEqualTo(GameType.RANKING_GAME);
    }

    private QuestionEntity question(UUID questionId) {
        QuestionEntity question = new QuestionEntity();
        question.setId(questionId);
        question.setText("Question");
        question.setCategory("test");
        question.setActive(true);
        return question;
    }
}
