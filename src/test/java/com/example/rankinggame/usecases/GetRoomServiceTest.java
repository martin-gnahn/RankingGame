package com.example.rankinggame.usecases;

import com.example.rankinggame.entities.Player;
import com.example.rankinggame.entities.PlayerConnectionStatus;
import com.example.rankinggame.entities.Room;
import com.example.rankinggame.entities.RoomStatus;
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
        Room room = new Room();
        room.setId(roomId);
        room.setCode("ABCD12");
        room.setStatus(RoomStatus.LOBBY);
        room.setHostPlayerId(hostId);
        Player host = player(hostId, roomId, "Marta", false);
        Player guest = player(playerId, roomId, "Alex", true);

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

    private Player player(UUID playerId, UUID roomId, String nickname, boolean host) {
        Player player = new Player();
        player.setId(playerId);
        player.setRoomId(roomId);
        player.setNickname(nickname);
        player.setHost(host);
        player.setConnectionStatus(PlayerConnectionStatus.CONNECTED);
        return player;
    }
}
