package com.example.rankinggame.usecases;

import com.example.rankinggame.entities.Player;
import com.example.rankinggame.entities.PlayerConnectionStatus;
import com.example.rankinggame.entities.Room;
import com.example.rankinggame.entities.RoomStatus;
import com.example.rankinggame.repositories.PlayerRepository;
import com.example.rankinggame.repositories.RoomRepository;
import com.example.rankinggame.services.RoomCodeGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateRoomServiceTest {
    @Test
    void createsLobbyRoomWithConnectedHostPlayer() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        RoomCodeGenerator roomCodeGenerator = mock(RoomCodeGenerator.class);
        UUID roomId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();

        when(roomCodeGenerator.generateUniqueCode()).thenReturn("ABCD12");
        when(roomRepository.save(ArgumentMatchers.any(Room.class))).thenAnswer(invocation -> {
            Room room = invocation.getArgument(0);
            if (room.getId() == null) {
                Room persistedRoom = new Room();
                persistedRoom.setId(roomId);
                persistedRoom.setCode(room.getCode());
                persistedRoom.setHostPlayerId(room.getHostPlayerId());
                persistedRoom.setStatus(room.getStatus());
                return persistedRoom;
            }
            return room;
        });
        when(playerRepository.save(ArgumentMatchers.any(Player.class))).thenAnswer(invocation -> {
            Player player = invocation.getArgument(0);
            player.setId(playerId);
            return player;
        });

        CreateRoomService service = new CreateRoomService(roomRepository, playerRepository, roomCodeGenerator);

        CreateRoomResult result = service.createRoom(new CreateRoomCommand("  Marta  "));

        assertThat(result.roomCode()).isEqualTo("ABCD12");
        assertThat(result.playerId()).isEqualTo(playerId);

        ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository, times(2)).save(roomCaptor.capture());
        assertThat(roomCaptor.getAllValues().get(0))
                .satisfies(room -> {
                    assertThat(room.getCode()).isEqualTo("ABCD12");
                    assertThat(room.getStatus()).isEqualTo(RoomStatus.LOBBY);
                    assertThat(room.getHostPlayerId()).isNull();
                });
        assertThat(roomCaptor.getAllValues().get(1).getHostPlayerId()).isEqualTo(playerId);

        ArgumentCaptor<Player> playerCaptor = ArgumentCaptor.forClass(Player.class);
        verify(playerRepository).save(playerCaptor.capture());
        assertThat(playerCaptor.getValue())
                .satisfies(player -> {
                    assertThat(player.getRoomId()).isEqualTo(roomId);
                    assertThat(player.getNickname()).isEqualTo("Marta");
                    assertThat(player.isHost()).isTrue();
                    assertThat(player.getConnectionStatus()).isEqualTo(PlayerConnectionStatus.CONNECTED);
                });

        InOrder inOrder = inOrder(roomRepository, playerRepository);
        inOrder.verify(roomRepository).save(ArgumentMatchers.any(Room.class));
        inOrder.verify(playerRepository).save(ArgumentMatchers.any(Player.class));
        inOrder.verify(roomRepository).save(ArgumentMatchers.any(Room.class));
    }

    @Test
    void rejectsBlankPlayerName() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        RoomCodeGenerator roomCodeGenerator = mock(RoomCodeGenerator.class);
        CreateRoomService service = new CreateRoomService(roomRepository, playerRepository, roomCodeGenerator);

        assertThatThrownBy(() -> service.createRoom(new CreateRoomCommand("   ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Player name is required");

        verify(roomCodeGenerator, never()).generateUniqueCode();
        verify(roomRepository, never()).save(ArgumentMatchers.any(Room.class));
        verify(playerRepository, never()).save(ArgumentMatchers.any(Player.class));
    }
}
