package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.PlayerConnectionStatus;
import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.entities.RoomEntity;
import com.example.rankinggame.entities.RoomStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaPlayerRepositoryTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    private static final String TOKEN_HASH = "Token-Hash";

    @Autowired
    private JpaRoomRepository roomRepository;

    @Autowired
    private JpaPlayerRepository playerRepository;

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void savesAndLoadsPlayerByRoomId() {
        RoomEntity room = new RoomEntity();
        room.setId(java.util.UUID.randomUUID());
        room.setCode("PLY1");
        room.setStatus(RoomStatus.LOBBY);
        RoomEntity savedRoom = roomRepository.saveAndFlush(room);

        PlayerEntity player = new PlayerEntity();
        player.setSessionExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        player.setTokenHash(TOKEN_HASH);
        player.setRoomId(savedRoom.getId());
        player.setNickname("Marta");
        player.setConnectionStatus(PlayerConnectionStatus.CONNECTED);

        PlayerEntity savedPlayer = playerRepository.saveAndFlush(player);

        assertThat(savedPlayer.getId()).isNotNull();
        assertThat(savedPlayer.getJoinedAt()).isNotNull();
        assertThat(((PlayerRepository) playerRepository).findById(savedPlayer.getId()))
                .isPresent()
                .get()
                .satisfies(foundPlayer -> {
                    assertThat(foundPlayer.getRoomId()).isEqualTo(savedRoom.getId());
                    assertThat(foundPlayer.getNickname()).isEqualTo("Marta");
                    assertThat(foundPlayer.getConnectionStatus()).isEqualTo(PlayerConnectionStatus.CONNECTED);
                });
        assertThat(playerRepository.findByRoomId(savedRoom.getId()))
                .extracting(PlayerEntity::getId)
                .containsExactly(savedPlayer.getId());
    }

    @Test
    void rejectsDuplicateNicknameInSameRoomIgnoringCase() {
        RoomEntity room = new RoomEntity();
        room.setId(java.util.UUID.randomUUID());
        room.setCode("DUP1");
        room.setStatus(RoomStatus.LOBBY);
        RoomEntity savedRoom = roomRepository.saveAndFlush(room);

        PlayerEntity firstPlayer = new PlayerEntity();
        firstPlayer.setSessionExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        firstPlayer.setTokenHash(TOKEN_HASH);
        firstPlayer.setRoomId(savedRoom.getId());
        firstPlayer.setNickname("Alex");
        firstPlayer.setConnectionStatus(PlayerConnectionStatus.CONNECTED);
        playerRepository.saveAndFlush(firstPlayer);

        PlayerEntity duplicatePlayer = new PlayerEntity();
        duplicatePlayer.setRoomId(savedRoom.getId());
        duplicatePlayer.setNickname("alex");
        duplicatePlayer.setConnectionStatus(PlayerConnectionStatus.CONNECTED);
        duplicatePlayer.setSessionExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        duplicatePlayer.setTokenHash("Different-Token-Hash");

        assertThatThrownBy(() -> playerRepository.saveAndFlush(duplicatePlayer))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicateSessionTokenHashInSameRoom() {
        RoomEntity room = new RoomEntity();
        room.setId(java.util.UUID.randomUUID());
        room.setCode("DUP2");
        room.setStatus(RoomStatus.LOBBY);
        RoomEntity savedRoom = roomRepository.saveAndFlush(room);

        PlayerEntity firstPlayer = new PlayerEntity();
        firstPlayer.setSessionExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        firstPlayer.setTokenHash(TOKEN_HASH);
        firstPlayer.setRoomId(savedRoom.getId());
        firstPlayer.setNickname("Alex");
        firstPlayer.setConnectionStatus(PlayerConnectionStatus.CONNECTED);
        playerRepository.saveAndFlush(firstPlayer);

        PlayerEntity duplicateTokenPlayer = new PlayerEntity();
        duplicateTokenPlayer.setSessionExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        duplicateTokenPlayer.setTokenHash(TOKEN_HASH);
        duplicateTokenPlayer.setRoomId(savedRoom.getId());
        duplicateTokenPlayer.setNickname("Marta");
        duplicateTokenPlayer.setConnectionStatus(PlayerConnectionStatus.CONNECTED);

        assertThatThrownBy(() -> playerRepository.saveAndFlush(duplicateTokenPlayer))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
