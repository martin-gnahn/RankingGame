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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaGameSessionPlayerRepositoryTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private JpaRoomRepository roomRepository;

    @Autowired
    private JpaPlayerRepository playerRepository;

    @Autowired
    private JpaGameSessionRepository gameSessionRepository;

    @Autowired
    private JpaGameSessionPlayerRepository gameSessionPlayerRepository;

    @Autowired
    private EntityManager entityManager;

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void savesParticipantSnapshotAndLoadsPlayersByGameSessionId() {
        RoomEntity room = new RoomEntity();
        room.setId(UUID.randomUUID());
        room.setCode("GSP1");
        room.setStatus(RoomStatus.LOBBY);
        RoomEntity savedRoom = roomRepository.saveAndFlush(room);

        PlayerEntity host = player(savedRoom, "Host", "Host-Token-Hash");
        PlayerEntity guest = player(savedRoom, "Guest", "Guest-Token-Hash");
        PlayerEntity savedHost = playerRepository.saveAndFlush(host);
        PlayerEntity savedGuest = playerRepository.saveAndFlush(guest);

        GameSession gameSession = new GameSession();
        gameSession.setId(UUID.randomUUID());
        gameSession.setRoomId(savedRoom.getId());
        gameSession.setGameType(GameType.RANKING_GAME);
        gameSession.setStatus(GameSessionStatus.IN_PROGRESS);
        gameSession.setCurrentRoundIndex(0);
        gameSession.setCurrentRoundId(UUID.randomUUID());
        GameSession savedGameSession = gameSessionRepository.saveAndFlush(gameSession);

        gameSessionPlayerRepository.saveAllAndFlush(java.util.List.of(
                new GameSessionPlayerEntity(savedGameSession.getId(), savedHost.getId()),
                new GameSessionPlayerEntity(savedGameSession.getId(), savedGuest.getId())
        ));

        entityManager.clear();

        assertThat(gameSessionPlayerRepository.findByGameSessionId(savedGameSession.getId()))
                .extracting(GameSessionPlayerEntity::getPlayerId)
                .containsExactlyInAnyOrder(savedHost.getId(), savedGuest.getId());
        assertThat(playerRepository.findByGameSessionId(savedGameSession.getId()))
                .extracting(PlayerEntity::getId)
                .containsExactly(savedHost.getId(), savedGuest.getId());
    }

    private PlayerEntity player(RoomEntity room, String nickname, String tokenHash) {
        PlayerEntity player = new PlayerEntity();
        player.setRoomId(room.getId());
        player.setNickname(nickname);
        player.setConnectionStatus(PlayerConnectionStatus.CONNECTED);
        player.setSessionExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        player.setTokenHash(tokenHash);
        return player;
    }
}
