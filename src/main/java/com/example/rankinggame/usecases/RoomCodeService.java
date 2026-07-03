package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.RoomCommand;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class RoomCodeService {
    String normalizeRoomCode(RoomCommand command) {
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
