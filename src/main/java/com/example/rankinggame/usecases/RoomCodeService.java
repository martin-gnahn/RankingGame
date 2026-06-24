package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.JoinRoomCommand;
import com.example.rankinggame.dto.StartRankingGameCommand;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class RoomCodeService {
    String normalizeRoomCode(StartRankingGameCommand command) {
        return normalize(command == null ? null : command.roomCode());
    }

    String normalizeRoomCode(JoinRoomCommand command) {
        return normalize(command == null ? null : command.roomCode());
    }

    private static @NonNull String normalize(String rawRoomCode) {
        if (rawRoomCode == null) {
            throw new RoomCodeRequiredException();
        }

        String roomCode = rawRoomCode.trim().toUpperCase(Locale.ROOT);

        if (roomCode.isBlank()) {
            throw new RoomCodeRequiredException();
        }

        return roomCode;
    }
}
