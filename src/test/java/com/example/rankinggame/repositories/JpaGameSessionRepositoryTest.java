package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.*;
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

import java.util.UUID;

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
        gameSession.setId(UUID.randomUUID());
        gameSession.setCurrentRoundId(UUID.randomUUID());
        gameSession.setRoomId(savedRoom.getId());
        gameSession.setGameType(GameType.RANKING_GAME);
        gameSession.setStatus(GameSessionStatus.IN_PROGRESS);
        gameSession.setCurrentRoundIndex(0);
        GameSession savedGameSession = gameSessionRepository.saveAndFlush(gameSession);

        QuestionEntity question = new QuestionEntity();
        question.setText("Wer wuerde am ehesten den WLAN-Namen aendern?");
        question.setActive(true);
        QuestionEntity savedQuestion = questionRepository.saveAndFlush(question);

        RoundEntity round = new RoundEntity();
        round.setGameSessionId(savedGameSession.getId());
        round.setQuestionId(savedQuestion.getId());
        round.setCaptainPlayerId(savedPlayer.getId());
        round.setState(RoundState.ANSWER_SUBMISSION);
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
                    assertThat(foundSession.getCurrentRoundIndex()).isZero();
                });
        assertThat(roundRepository.findByGameSessionId(savedGameSession.getId()))
                .singleElement()
                .extracting(RoundEntity::getId)
                .isEqualTo(savedRound.getId());
    }
}
