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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class GetRoomService {
    private static final String ROOM_NOT_IN_LOBBY_REASON = "Room is not in lobby";
    private static final String HOST_OFFLINE_REASON = "Host must be online to start the game";
    private static final String NOT_ENOUGH_PLAYERS_REASON = "At least 2 players are required to start the game";

    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;

    // TODO: Do we need transactions here.
    @Transactional(readOnly = true)
    public RoomDetailsResult getRoom(String rawRoomCode) {
        String roomCode = normalizeRoomCode(rawRoomCode);
        RoomEntity room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RoomNotFoundException(roomCode));

        // TODO: maybe extract the complex sorting logic to outer method.
        List<PlayerEntity> playerEntities = playerRepository.findByRoomId(room.getId());
        List<PlayerDetailsResult> players = playerEntities.stream()
                .sorted(Comparator.comparing((PlayerEntity player) -> isHost(room, player)).reversed()
                        .thenComparing(PlayerEntity::getJoinedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(PlayerEntity::getNickname))
                .map(player -> new PlayerDetailsResult(
                        player.getId(),
                        player.getNickname(),
                        isHost(room, player),
                        player.getConnectionStatus()
                ))
                .toList();

        StartEligibility startEligibility = startEligibility(room, playerEntities);
        return new RoomDetailsResult(
                room.getId(),
                room.getCode(),
                room.getStatus(),
                players,
                startEligibility.canStartGame(),
                startEligibility.blockedReason()
        );
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

    private boolean isHost(RoomEntity room, PlayerEntity player) {
        return Objects.equals(room.getHostPlayerId(), player.getId());
    }

    private StartEligibility startEligibility(RoomEntity room, List<PlayerEntity> players) {
        if (room.getStatus() != RoomStatus.LOBBY) {
            return StartEligibility.blocked(ROOM_NOT_IN_LOBBY_REASON);
        }

        boolean hostConnected = players.stream()
                .filter(player -> isHost(room, player))
                .anyMatch(this::isConnected);
        if (!hostConnected) {
            return StartEligibility.blocked(HOST_OFFLINE_REASON);
        }

        boolean hasConnectedNonHost = players.stream()
                .filter(player -> !isHost(room, player))
                .anyMatch(this::isConnected);
        if (!hasConnectedNonHost) {
            return StartEligibility.blocked(NOT_ENOUGH_PLAYERS_REASON);
        }

        return StartEligibility.allowed();
    }

    private boolean isConnected(PlayerEntity player) {
        return player.getConnectionStatus() == PlayerConnectionStatus.CONNECTED;
    }

    private record StartEligibility(boolean canStartGame, String blockedReason) {
        static StartEligibility allowed() {
            return new StartEligibility(true, null);
        }

        static StartEligibility blocked(String reason) {
            return new StartEligibility(false, reason);
        }
    }
}
