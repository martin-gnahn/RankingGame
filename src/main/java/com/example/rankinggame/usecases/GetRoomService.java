package com.example.rankinggame.usecases;

import com.example.rankinggame.entities.Player;
import com.example.rankinggame.entities.Room;
import com.example.rankinggame.repositories.PlayerRepository;
import com.example.rankinggame.repositories.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
@Service
public class GetRoomService {
    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;

    @Transactional(readOnly = true)
    public RoomDetailsResult getRoom(String rawRoomCode) {
        String roomCode = normalizeRoomCode(rawRoomCode);
        Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RoomNotFoundException(roomCode));

        List<PlayerDetailsResult> players = playerRepository.findByRoomId(room.getId()).stream()
                .sorted(Comparator.comparing(Player::isHost).reversed()
                        .thenComparing(Player::getJoinedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Player::getNickname))
                .map(player -> new PlayerDetailsResult(
                        player.getId(),
                        player.getNickname(),
                        player.isHost(),
                        player.getConnectionStatus()
                ))
                .toList();

        return new RoomDetailsResult(room.getId(), room.getCode(), room.getStatus(), players);
    }

    private String normalizeRoomCode(String rawRoomCode) {
        if (rawRoomCode == null) {
            throw new IllegalArgumentException("Room code is required");
        }

        String roomCode = rawRoomCode.trim().toUpperCase(Locale.ROOT);

        if (roomCode.isBlank()) {
            throw new IllegalArgumentException("Room code is required");
        }

        return roomCode;
    }
}
