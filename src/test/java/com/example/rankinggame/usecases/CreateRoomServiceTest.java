package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.CreateRoomCommand;
import com.example.rankinggame.dto.CreateRoomResult;
import com.example.rankinggame.entities.PlayerConnectionStatus;
import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.entities.RoomEntity;
import com.example.rankinggame.entities.RoomStatus;
import com.example.rankinggame.exceptions.RoomCodeUnavailableException;
import com.example.rankinggame.repositories.PlayerRepository;
import com.example.rankinggame.repositories.RoomRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CreateRoomServiceTest {
    private static final TransactionOperations NO_TRANSACTION = new TransactionOperations() {
        @Override
        public <T> T execute(TransactionCallback<T> action) {
            return action.doInTransaction(nullTransactionStatus());
        }

        private TransactionStatus nullTransactionStatus() {
            return null;
        }
    };

    @Test
    void createsLobbyRoomWithConnectedHostPlayer() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        RoomCodeGenerator roomCodeGenerator = mock(RoomCodeGenerator.class);
        UUID playerId = UUID.randomUUID();

        when(roomCodeGenerator.generateUniqueCode()).thenReturn("ABCD12");
        when(roomRepository.save(ArgumentMatchers.any(RoomEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(playerRepository.save(ArgumentMatchers.any(PlayerEntity.class))).thenAnswer(invocation -> {
            PlayerEntity player = invocation.getArgument(0);
            player.setId(playerId);
            return player;
        });

        CreateRoomService service = new CreateRoomService(roomRepository, playerRepository, roomCodeGenerator, NO_TRANSACTION);

        CreateRoomResult result = service.createRoom(new CreateRoomCommand("  Marta  "));

        assertThat(result.roomCode()).isEqualTo("ABCD12");
        assertThat(result.roomId()).isNotNull();
        assertThat(result.playerId()).isEqualTo(playerId);
        assertThat(result.playerName()).isEqualTo("Marta");

        ArgumentCaptor<RoomEntity> roomCaptor = ArgumentCaptor.forClass(RoomEntity.class);
        verify(roomRepository, times(2)).save(roomCaptor.capture());
        assertThat(roomCaptor.getAllValues().get(0))
                .satisfies(room -> {
                    assertThat(room.getId()).isEqualTo(result.roomId());
                    assertThat(room.getCode()).isEqualTo("ABCD12");
                    assertThat(room.getStatus()).isEqualTo(RoomStatus.LOBBY);
                });
        assertThat(roomCaptor.getAllValues().get(1).getHostPlayerId()).isEqualTo(playerId);

        ArgumentCaptor<PlayerEntity> playerCaptor = ArgumentCaptor.forClass(PlayerEntity.class);
        verify(playerRepository).save(playerCaptor.capture());
        assertThat(playerCaptor.getValue())
                .satisfies(player -> {
                    assertThat(player.getRoomId()).isEqualTo(result.roomId());
                    assertThat(player.getNickname()).isEqualTo("Marta");
                    assertThat(player.getConnectionStatus()).isEqualTo(PlayerConnectionStatus.CONNECTED);
                });

        InOrder inOrder = inOrder(roomRepository, playerRepository);
        inOrder.verify(roomRepository).save(ArgumentMatchers.any(RoomEntity.class));
        inOrder.verify(roomRepository).flush();
        inOrder.verify(playerRepository).save(ArgumentMatchers.any(PlayerEntity.class));
        inOrder.verify(roomRepository).save(ArgumentMatchers.any(RoomEntity.class));
    }

    @Test
    void retriesWhenRoomCodeInsertCollides() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        RoomCodeGenerator roomCodeGenerator = mock(RoomCodeGenerator.class);
        UUID playerId = UUID.randomUUID();

        when(roomCodeGenerator.generateUniqueCode()).thenReturn("DUPL1", "UNIQ2");
        when(roomRepository.save(ArgumentMatchers.any(RoomEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new DataIntegrityViolationException("duplicate code"))
                .doNothing()
                .when(roomRepository).flush();
        when(playerRepository.save(ArgumentMatchers.any(PlayerEntity.class))).thenAnswer(invocation -> {
            PlayerEntity player = invocation.getArgument(0);
            player.setId(playerId);
            return player;
        });

        CreateRoomService service = new CreateRoomService(roomRepository, playerRepository, roomCodeGenerator, NO_TRANSACTION);

        CreateRoomResult result = service.createRoom(new CreateRoomCommand("Marta"));

        assertThat(result.roomCode()).isEqualTo("UNIQ2");
        assertThat(result.roomId()).isNotNull();
        assertThat(result.playerId()).isEqualTo(playerId);
        verify(roomCodeGenerator, times(2)).generateUniqueCode();
        verify(roomRepository, times(2)).flush();
        verify(playerRepository).save(ArgumentMatchers.any(PlayerEntity.class));
    }

    @Test
    void failsClearlyWhenRoomCodeInsertKeepsColliding() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        RoomCodeGenerator roomCodeGenerator = mock(RoomCodeGenerator.class);

        when(roomCodeGenerator.generateUniqueCode()).thenReturn("DUPL1");
        when(roomRepository.save(ArgumentMatchers.any(RoomEntity.class))).thenAnswer(invocation -> {
            RoomEntity room = invocation.getArgument(0);
            room.setId(UUID.randomUUID());
            return room;
        });
        when(playerRepository.save(ArgumentMatchers.any(PlayerEntity.class))).thenAnswer(invocation -> {
            PlayerEntity player = invocation.getArgument(0);
            player.setId(UUID.randomUUID());
            return player;
        });
        doThrow(new DataIntegrityViolationException("duplicate code")).when(roomRepository).flush();

        CreateRoomService service = new CreateRoomService(roomRepository, playerRepository, roomCodeGenerator, NO_TRANSACTION);

        assertThatThrownBy(() -> service.createRoom(new CreateRoomCommand("Marta")))
                .isInstanceOf(RoomCodeUnavailableException.class)
                .hasMessage("Unable to allocate a unique room code")
                .hasCauseInstanceOf(RoomCodeCollisionException.class)
                .hasRootCauseInstanceOf(DataIntegrityViolationException.class);

        verify(roomCodeGenerator, times(5)).generateUniqueCode();
        verify(playerRepository, never()).save(ArgumentMatchers.any(PlayerEntity.class));
    }

    @Test
    void doesNotRetryUnrelatedPlayerIntegrityFailures() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        RoomCodeGenerator roomCodeGenerator = mock(RoomCodeGenerator.class);
        when(roomCodeGenerator.generateUniqueCode()).thenReturn("ABCD12");
        when(roomRepository.save(ArgumentMatchers.any(RoomEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new DataIntegrityViolationException("player constraint"))
                .when(playerRepository).save(ArgumentMatchers.any(PlayerEntity.class));

        CreateRoomService service = new CreateRoomService(roomRepository, playerRepository, roomCodeGenerator, NO_TRANSACTION);

        assertThatThrownBy(() -> service.createRoom(new CreateRoomCommand("Marta")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("player constraint");

        verify(roomCodeGenerator).generateUniqueCode();
        verify(playerRepository).save(ArgumentMatchers.any(PlayerEntity.class));
    }

    @Test
    void rejectsBlankPlayerName() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        RoomCodeGenerator roomCodeGenerator = mock(RoomCodeGenerator.class);
        CreateRoomService service = new CreateRoomService(roomRepository, playerRepository, roomCodeGenerator, NO_TRANSACTION);

        assertThatThrownBy(() -> service.createRoom(new CreateRoomCommand("   ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Player name is required");

        verify(roomCodeGenerator, never()).generateUniqueCode();
        verify(roomRepository, never()).save(ArgumentMatchers.any(RoomEntity.class));
        verify(playerRepository, never()).save(ArgumentMatchers.any(PlayerEntity.class));
    }

}
