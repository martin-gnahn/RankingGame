package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.GameSession;
import com.example.rankinggame.entities.GameSessionStatus;
import com.example.rankinggame.entities.GameType;
import com.example.rankinggame.entities.PlayerConnectionStatus;
import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.entities.QuestionEntity;
import com.example.rankinggame.entities.RoomEntity;
import com.example.rankinggame.entities.RoomStatus;
import com.example.rankinggame.entities.RoundEntity;
import com.example.rankinggame.entities.RoundState;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaGameSessionRepositoryTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private JpaRoomRepository roomRepository;

    @Autowired
    private JpaGameSessionRepository gameSessionRepository;

    @Autowired
    private JpaPlayerRepository playerRepository;

    @Autowired
    private JpaQuestionRepository questionRepository;

    @Autowired
    private JpaRoundRepository roundRepository;

    @Autowired
    private EntityManager entityManager;

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void savesAndLoadsGameSessionByRoomId() {
        RoomEntity room = new RoomEntity();
        room.setCode("GMS1");
        room.setStatus(RoomStatus.LOBBY);
        RoomEntity savedRoom = roomRepository.saveAndFlush(room);

        PlayerEntity player = new PlayerEntity();
        player.setRoomId(savedRoom.getId());
        player.setNickname("Marta");
        player.setHost(true);
        player.setConnectionStatus(PlayerConnectionStatus.CONNECTED);
        PlayerEntity savedPlayer = playerRepository.saveAndFlush(player);

        GameSession gameSession = new GameSession();
        gameSession.setRoomId(savedRoom.getId());
        gameSession.setGameType(GameType.RANKING_GAME);
        gameSession.setStatus(GameSessionStatus.IN_PROGRESS);
        gameSession.setCurrentRoundNumber(1);
        gameSession.setPlayers(List.of(savedPlayer));
        GameSession savedGameSession = gameSessionRepository.saveAndFlush(gameSession);

        QuestionEntity question = new QuestionEntity();
        question.setText("Wer wuerde am ehesten den WLAN-Namen aendern?");
        question.setActive(true);
        QuestionEntity savedQuestion = questionRepository.saveAndFlush(question);

        RoundEntity round = new RoundEntity();
        round.setGameSessionId(savedGameSession.getId());
        round.setQuestionId(savedQuestion.getId());
        round.setCaptainPlayerId(savedPlayer.getId());
        round.setRoundNumber(1);
        round.setState(RoundState.QUESTION_REVEALED);
        RoundEntity savedRound = roundRepository.saveAndFlush(round);

        entityManager.clear();

        assertThat(savedGameSession.getId()).isNotNull();
        assertThat(gameSessionRepository.findByRoomId(savedRoom.getId()))
                .isPresent()
                .get()
                .satisfies(foundSession -> {
                    assertThat(foundSession.getId()).isEqualTo(savedGameSession.getId());
                    assertThat(foundSession.getGameType()).isEqualTo(GameType.RANKING_GAME);
                    assertThat(foundSession.getStatus()).isEqualTo(GameSessionStatus.IN_PROGRESS);
                    assertThat(foundSession.getCurrentRoundNumber()).isEqualTo(1);
                    assertThat(foundSession.getPlayers())
                            .singleElement()
                            .extracting(PlayerEntity::getId)
                            .isEqualTo(savedPlayer.getId());
                    assertThat(foundSession.getRounds())
                            .singleElement()
                            .extracting(RoundEntity::getId)
                            .isEqualTo(savedRound.getId());
                });
    }
}
