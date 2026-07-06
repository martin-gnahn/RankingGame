package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.RoomEntity;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaRoomRepositoryTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private JpaRoomRepository roomRepository;

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void savesAndLoadsRoomByCode() {
        RoomEntity room = new RoomEntity();
        room.setId(UUID.randomUUID());
        room.setCode("ABCD");
        room.setStatus(RoomStatus.LOBBY);

        RoomEntity savedRoom = roomRepository.saveAndFlush(room);

        assertThat(savedRoom.getId()).isNotNull();
        assertThat(savedRoom.getCreatedAt()).isNotNull();
        assertThat(roomRepository.existsByCode("ABCD")).isTrue();
        assertThat(roomRepository.findByCode("ABCD"))
                .isPresent()
                .get()
                .satisfies(foundRoom -> {
                    assertThat(foundRoom.getId()).isEqualTo(savedRoom.getId());
                    assertThat(foundRoom.getCode()).isEqualTo("ABCD");
                    assertThat(foundRoom.getStatus()).isEqualTo(RoomStatus.LOBBY);
                    assertThat(foundRoom.getHostPlayerId()).isNull();
                });
    }
}
