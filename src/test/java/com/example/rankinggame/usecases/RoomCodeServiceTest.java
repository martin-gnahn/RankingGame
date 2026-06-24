package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.JoinRoomCommand;
import com.example.rankinggame.dto.StartRankingGameCommand;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomCodeServiceTest {
    private final RoomCodeService roomCodeService = new RoomCodeService();

    @Test
    void normalizesJoinRoomCode() {
        String roomCode = roomCodeService.normalizeRoomCode(new JoinRoomCommand(" abcd12 ", "Alex"));

        assertThat(roomCode).isEqualTo("ABCD12");
    }

    @Test
    void normalizesStartRankingGameRoomCode() {
        String roomCode = roomCodeService.normalizeRoomCode(new StartRankingGameCommand(" abcd12 ", UUID.randomUUID()));

        assertThat(roomCode).isEqualTo("ABCD12");
    }

    @Test
    void rejectsMissingJoinRoomCode() {
        assertThatThrownBy(() -> roomCodeService.normalizeRoomCode(new JoinRoomCommand("   ", "Alex")))
                .isInstanceOf(RoomCodeRequiredException.class)
                .hasMessage("Room code is required");
    }

    @Test
    void rejectsMissingStartRankingGameRoomCode() {
        assertThatThrownBy(() -> roomCodeService.normalizeRoomCode(new StartRankingGameCommand(null, UUID.randomUUID())))
                .isInstanceOf(RoomCodeRequiredException.class)
                .hasMessage("Room code is required");
    }
}
