package com.example.rankinggame.usecases;

import com.example.rankinggame.entities.Player;
import com.example.rankinggame.entities.PlayerConnectionStatus;
import com.example.rankinggame.entities.Room;
import com.example.rankinggame.entities.RoomStatus;
import com.example.rankinggame.repositories.PlayerRepository;
import com.example.rankinggame.repositories.RoomRepository;
import com.example.rankinggame.services.RoomCodeGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateRoomService {
    private static final int MAX_PLAYER_NAME_LENGTH = 80;

    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final RoomCodeGenerator roomCodeGenerator;

    public CreateRoomService(
            RoomRepository roomRepository,
            PlayerRepository playerRepository,
            RoomCodeGenerator roomCodeGenerator
    ) {
        this.roomRepository = roomRepository;
        this.playerRepository = playerRepository;
        this.roomCodeGenerator = roomCodeGenerator;
    }

    @Transactional
    public CreateRoomResult createRoom(CreateRoomCommand command) {
        String playerName = normalizePlayerName(command);

        Room room = new Room();
        room.setCode(roomCodeGenerator.generateUniqueCode());
        room.setStatus(RoomStatus.LOBBY);

        Room savedRoom = roomRepository.save(room);

        Player hostPlayer = new Player();
        hostPlayer.setRoomId(savedRoom.getId());
        hostPlayer.setNickname(playerName);
        hostPlayer.setHost(true);
        hostPlayer.setConnectionStatus(PlayerConnectionStatus.CONNECTED);

        Player savedHostPlayer = playerRepository.save(hostPlayer);
        savedRoom.setHostPlayerId(savedHostPlayer.getId());
        roomRepository.save(savedRoom);

        return new CreateRoomResult(savedRoom.getCode(), savedRoom.getId(), savedHostPlayer.getId(), savedHostPlayer.getNickname());
    }

    private String normalizePlayerName(CreateRoomCommand command) {
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
