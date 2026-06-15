package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.Player;
import com.example.rankinggame.entities.PlayerConnectionStatus;
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
class JpaPlayerRepositoryTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

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
        Room room = new Room();
        room.setCode("PLY1");
        room.setStatus(RoomStatus.LOBBY);
        Room savedRoom = roomRepository.saveAndFlush(room);

        Player player = new Player();
        player.setRoomId(savedRoom.getId());
        player.setNickname("Marta");
        player.setHost(true);
        player.setConnectionStatus(PlayerConnectionStatus.CONNECTED);

        Player savedPlayer = playerRepository.saveAndFlush(player);

        assertThat(savedPlayer.getId()).isNotNull();
        assertThat(savedPlayer.getJoinedAt()).isNotNull();
        assertThat(((PlayerRepository) playerRepository).findById(savedPlayer.getId()))
                .isPresent()
                .get()
                .satisfies(foundPlayer -> {
                    assertThat(foundPlayer.getRoomId()).isEqualTo(savedRoom.getId());
                    assertThat(foundPlayer.getNickname()).isEqualTo("Marta");
                    assertThat(foundPlayer.isHost()).isTrue();
                    assertThat(foundPlayer.getConnectionStatus()).isEqualTo(PlayerConnectionStatus.CONNECTED);
                });
        assertThat(playerRepository.findByRoomId(savedRoom.getId()))
                .extracting(Player::getId)
                .containsExactly(savedPlayer.getId());
    }
}
