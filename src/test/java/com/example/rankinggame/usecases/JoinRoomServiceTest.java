package com.example.rankinggame.usecases;

import com.example.rankinggame.auth.TokenGenerator;
import com.example.rankinggame.dto.JoinRoomCommand;
import com.example.rankinggame.dto.JoinRoomResult;
import com.example.rankinggame.entities.PlayerConnectionStatus;
import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.entities.RoomEntity;
import com.example.rankinggame.entities.RoomStatus;
import com.example.rankinggame.events.PlayerJoinedRoomEvent;
import com.example.rankinggame.exceptions.RoomNotFoundException;
import com.example.rankinggame.repositories.PlayerRepository;
import com.example.rankinggame.repositories.RoomRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class JoinRoomServiceTest {
    @Test
    void joinsLobbyRoomWithConnectedNonHostPlayer() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        UUID roomId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        RoomEntity room = new RoomEntity();
        room.setId(roomId);
        room.setCode("ABCD12");
        room.setStatus(RoomStatus.LOBBY);

        when(roomRepository.findByCode("ABCD12")).thenReturn(Optional.of(room));
        when(playerRepository.findByRoomId(roomId)).thenReturn(List.of());
        when(playerRepository.save(ArgumentMatchers.any(PlayerEntity.class))).thenAnswer(invocation -> {
            PlayerEntity player = invocation.getArgument(0);
            player.setId(playerId);
            return player;
        });

        JoinRoomService service = createService(roomRepository, playerRepository, eventPublisher);

        JoinRoomResult result = service.joinRoom(new JoinRoomCommand(" abcd12 ", "  Alex  "));

        assertThat(result.roomCode()).isEqualTo("ABCD12");
        assertThat(result.roomId()).isEqualTo(roomId);
        assertThat(result.playerId()).isEqualTo(playerId);
        assertThat(result.playerName()).isEqualTo("Alex");

        ArgumentCaptor<PlayerEntity> playerCaptor = ArgumentCaptor.forClass(PlayerEntity.class);
        verify(playerRepository).save(playerCaptor.capture());
        assertThat(playerCaptor.getValue())
                .satisfies(player -> {
                    assertThat(player.getRoomId()).isEqualTo(roomId);
                    assertThat(player.getNickname()).isEqualTo("Alex");
                    assertThat(player.getConnectionStatus()).isEqualTo(PlayerConnectionStatus.CONNECTED);
                    assertThat(player.getTokenHash()).isEqualTo("hashed-token");
                    assertThat(player.getSessionExpiresAt()).isAfter(Instant.now());
                });

        ArgumentCaptor<PlayerJoinedRoomEvent> eventCaptor = ArgumentCaptor.forClass(PlayerJoinedRoomEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .satisfies(event -> {
                    assertThat(event.roomCode()).isEqualTo("ABCD12");
                    assertThat(event.playerId()).isEqualTo(playerId);
                    assertThat(event.nickname()).isEqualTo("Alex");
                    assertThat(event.host()).isFalse();
                });
    }

    @Test
    void rejectsUnknownRoom() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        when(roomRepository.findByCode("MISS1")).thenReturn(Optional.empty());
        JoinRoomService service = createService(roomRepository, playerRepository, eventPublisher);

        assertThatThrownBy(() -> service.joinRoom(new JoinRoomCommand("MISS1", "Alex")))
                .isInstanceOf(RoomNotFoundException.class)
                .hasMessage("Room not found: MISS1");

        verify(playerRepository, never()).save(ArgumentMatchers.any(PlayerEntity.class));
    }

    @Test
    void rejectsDuplicateNicknameInRoom() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        UUID roomId = UUID.randomUUID();
        RoomEntity room = new RoomEntity();
        room.setId(roomId);
        room.setCode("ABCD12");
        room.setStatus(RoomStatus.LOBBY);
        PlayerEntity existingPlayer = new PlayerEntity();
        existingPlayer.setNickname("Alex");

        when(roomRepository.findByCode("ABCD12")).thenReturn(Optional.of(room));
        when(playerRepository.findByRoomId(roomId)).thenReturn(List.of(existingPlayer));
        JoinRoomService service = createService(roomRepository, playerRepository, eventPublisher);

        assertThatThrownBy(() -> service.joinRoom(new JoinRoomCommand("ABCD12", "alex")))
                .isInstanceOf(PlayerNameAlreadyTakenException.class)
                .hasMessage("Player name is already taken");

        verify(playerRepository, never()).save(ArgumentMatchers.any(PlayerEntity.class));
    }

    @Test
    void translatesDatabaseDuplicateNicknameRaceToDomainError() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        UUID roomId = UUID.randomUUID();
        RoomEntity room = new RoomEntity();
        room.setId(roomId);
        room.setCode("ABCD12");
        room.setStatus(RoomStatus.LOBBY);

        when(roomRepository.findByCode("ABCD12")).thenReturn(Optional.of(room));
        when(playerRepository.findByRoomId(roomId)).thenReturn(List.of());
        when(playerRepository.save(ArgumentMatchers.any(PlayerEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new DataIntegrityViolationException("duplicate nickname")).when(playerRepository).flush();
        JoinRoomService service = createService(roomRepository, playerRepository, eventPublisher);

        assertThatThrownBy(() -> service.joinRoom(new JoinRoomCommand("ABCD12", "Alex")))
                .isInstanceOf(PlayerNameAlreadyTakenException.class)
                .hasMessage("Player name is already taken");
    }

    private JoinRoomService createService(
            RoomRepository roomRepository,
            PlayerRepository playerRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        TokenGenerator tokenGenerator = mock(TokenGenerator.class);
        when(tokenGenerator.generateSafeToken()).thenReturn("raw-token");
        when(tokenGenerator.generateHashFromToken("raw-token")).thenReturn("hashed-token");
        return new JoinRoomService(
                roomRepository,
                playerRepository,
                eventPublisher,
                new RoomCodeService(),
                tokenGenerator
        );
    }
}
