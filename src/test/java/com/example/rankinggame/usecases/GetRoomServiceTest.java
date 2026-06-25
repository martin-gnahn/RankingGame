package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.PlayerDetailsResult;
import com.example.rankinggame.dto.RoomDetailsResult;
import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.entities.PlayerConnectionStatus;
import com.example.rankinggame.entities.RoomEntity;
import com.example.rankinggame.entities.RoomStatus;
import com.example.rankinggame.exceptions.RoomNotFoundException;
import com.example.rankinggame.repositories.PlayerRepository;
import com.example.rankinggame.repositories.RoomRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetRoomServiceTest {
    @Test
    void loadsRoomWithPlayers() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        UUID roomId = UUID.randomUUID();
        UUID hostId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        RoomEntity room = new RoomEntity();
        room.setId(roomId);
        room.setCode("ABCD12");
        room.setStatus(RoomStatus.LOBBY);
        room.setHostPlayerId(hostId);
        PlayerEntity host = player(hostId, roomId, "Marta", false);
        PlayerEntity guest = player(playerId, roomId, "Alex", true);

        when(roomRepository.findByCode("ABCD12")).thenReturn(Optional.of(room));
        when(playerRepository.findByRoomId(roomId)).thenReturn(List.of(guest, host));
        GetRoomService service = new GetRoomService(roomRepository, playerRepository);

        RoomDetailsResult result = service.getRoom(" abcd12 ");

        assertThat(result.roomId()).isEqualTo(roomId);
        assertThat(result.roomCode()).isEqualTo("ABCD12");
        assertThat(result.status()).isEqualTo(RoomStatus.LOBBY);
        assertThat(result.players())
                .extracting(PlayerDetailsResult::playerId)
                .containsExactly(hostId, playerId);
        assertThat(result.players())
                .extracting(PlayerDetailsResult::host)
                .containsExactly(true, false);
        assertThat(result.canStartGame()).isTrue();
        assertThat(result.startBlockedReason()).isNull();
    }

    @Test
    void blocksStartWhenNoConnectedNonHostPlayerIsOnline() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        UUID roomId = UUID.randomUUID();
        UUID hostId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        RoomEntity room = new RoomEntity();
        room.setId(roomId);
        room.setCode("ABCD12");
        room.setStatus(RoomStatus.LOBBY);
        room.setHostPlayerId(hostId);
        PlayerEntity host = player(hostId, roomId, "Marta", true);
        PlayerEntity guest = player(playerId, roomId, "Alex", false);
        guest.setConnectionStatus(PlayerConnectionStatus.DISCONNECTED);

        when(roomRepository.findByCode("ABCD12")).thenReturn(Optional.of(room));
        when(playerRepository.findByRoomId(roomId)).thenReturn(List.of(host, guest));
        GetRoomService service = new GetRoomService(roomRepository, playerRepository);

        RoomDetailsResult result = service.getRoom("ABCD12");

        assertThat(result.canStartGame()).isFalse();
        assertThat(result.startBlockedReason()).isEqualTo("At least 2 players are required to start the game");
    }

    @Test
    void blocksStartOutsideLobby() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        UUID roomId = UUID.randomUUID();
        UUID hostId = UUID.randomUUID();
        RoomEntity room = new RoomEntity();
        room.setId(roomId);
        room.setCode("ABCD12");
        room.setStatus(RoomStatus.IN_GAME);
        room.setHostPlayerId(hostId);

        when(roomRepository.findByCode("ABCD12")).thenReturn(Optional.of(room));
        when(playerRepository.findByRoomId(roomId)).thenReturn(List.of(player(hostId, roomId, "Marta", true)));
        GetRoomService service = new GetRoomService(roomRepository, playerRepository);

        RoomDetailsResult result = service.getRoom("ABCD12");

        assertThat(result.canStartGame()).isFalse();
        assertThat(result.startBlockedReason()).isEqualTo("Room is not in lobby");
    }

    @Test
    void rejectsUnknownRoom() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        when(roomRepository.findByCode("MISS1")).thenReturn(Optional.empty());
        GetRoomService service = new GetRoomService(roomRepository, playerRepository);

        assertThatThrownBy(() -> service.getRoom("MISS1"))
                .isInstanceOf(RoomNotFoundException.class)
                .hasMessage("Room not found: MISS1");
    }

    private PlayerEntity player(UUID playerId, UUID roomId, String nickname, boolean host) {
        PlayerEntity player = new PlayerEntity();
        player.setId(playerId);
        player.setRoomId(roomId);
        player.setNickname(nickname);
        player.setHost(host);
        player.setConnectionStatus(PlayerConnectionStatus.CONNECTED);
        return player;
    }
}
