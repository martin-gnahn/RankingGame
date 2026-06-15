package com.example.rankinggame.usecases;

import com.example.rankinggame.entities.Player;
import com.example.rankinggame.entities.PlayerConnectionStatus;
import com.example.rankinggame.entities.Room;
import com.example.rankinggame.entities.RoomStatus;
import com.example.rankinggame.repositories.PlayerRepository;
import com.example.rankinggame.repositories.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class JoinRoomService {
    private static final int MAX_PLAYER_NAME_LENGTH = 80;

    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;

    public JoinRoomService(RoomRepository roomRepository, PlayerRepository playerRepository) {
        this.roomRepository = roomRepository;
        this.playerRepository = playerRepository;
    }

    @Transactional
    public JoinRoomResult joinRoom(JoinRoomCommand command) {
        String roomCode = normalizeRoomCode(command);
        String playerName = normalizePlayerName(command);

        Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RoomNotFoundException(roomCode));

        if (room.getStatus() != RoomStatus.LOBBY) {
            throw new IllegalArgumentException("Room is not open for joining");
        }

        boolean nicknameTaken = playerRepository.findByRoomId(room.getId()).stream()
                .anyMatch(player -> player.getNickname().equalsIgnoreCase(playerName));

        if (nicknameTaken) {
            throw new IllegalArgumentException("Player name is already taken");
        }

        Player player = new Player();
        player.setRoomId(room.getId());
        player.setNickname(playerName);
        player.setHost(false);
        player.setConnectionStatus(PlayerConnectionStatus.CONNECTED);

        Player savedPlayer = playerRepository.save(player);

        return new JoinRoomResult(room.getCode(), room.getId(), savedPlayer.getId(), savedPlayer.getNickname());
    }

    private String normalizeRoomCode(JoinRoomCommand command) {
        if (command == null || command.roomCode() == null) {
            throw new IllegalArgumentException("Room code is required");
        }

        String roomCode = command.roomCode().trim().toUpperCase(Locale.ROOT);

        if (roomCode.isBlank()) {
            throw new IllegalArgumentException("Room code is required");
        }

        return roomCode;
    }

    private String normalizePlayerName(JoinRoomCommand command) {
        if (command == null || command.playerName() == null) {
            throw new IllegalArgumentException("Player name is required");
        }

        String playerName = command.playerName().trim();

        if (playerName.isBlank()) {
            throw new IllegalArgumentException("Player name is required");
        }

        if (playerName.length() > MAX_PLAYER_NAME_LENGTH) {
            throw new IllegalArgumentException("Player name must be 80 characters or fewer");
        }

        return playerName;
    }
}
