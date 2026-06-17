package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.GameSession;
import com.example.rankinggame.entities.GameSessionStatus;
import com.example.rankinggame.entities.GameType;
import com.example.rankinggame.entities.Room;
import com.example.rankinggame.entities.RoomStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void savesAndLoadsGameSessionByRoomId() {
        Room room = new Room();
        room.setCode("GMS1");
        room.setStatus(RoomStatus.LOBBY);
        Room savedRoom = roomRepository.saveAndFlush(room);

        GameSession gameSession = new GameSession();
        gameSession.setRoomId(savedRoom.getId());
        gameSession.setGameType(GameType.RANKING_GAME);
        gameSession.setStatus(GameSessionStatus.IN_PROGRESS);
        gameSession.setCurrentRoundNumber(1);
        GameSession savedGameSession = gameSessionRepository.saveAndFlush(gameSession);

        assertThat(savedGameSession.getId()).isNotNull();
        assertThat(gameSessionRepository.findByRoomId(savedRoom.getId()))
                .isPresent()
                .get()
                .satisfies(foundSession -> {
                    assertThat(foundSession.getId()).isEqualTo(savedGameSession.getId());
                    assertThat(foundSession.getGameType()).isEqualTo(GameType.RANKING_GAME);
                    assertThat(foundSession.getStatus()).isEqualTo(GameSessionStatus.IN_PROGRESS);
                    assertThat(foundSession.getCurrentRoundNumber()).isEqualTo(1);
                });
    }
}
