package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.CreateRoomCommand;
import com.example.rankinggame.dto.CreateRoomResult;
import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.entities.PlayerConnectionStatus;
import com.example.rankinggame.entities.RoomEntity;
import com.example.rankinggame.entities.RoomStatus;
import com.example.rankinggame.exceptions.RoomCodeUnavailableException;
import com.example.rankinggame.repositories.PlayerRepository;
import com.example.rankinggame.repositories.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CreateRoomService {
    private static final int MAX_PLAYER_NAME_LENGTH = 80;
    private static final int MAX_ROOM_CREATION_ATTEMPTS = 5;

    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final RoomCodeGenerator roomCodeGenerator;
    private final TransactionOperations transactionOperations;

    @Autowired
    public CreateRoomService(
            RoomRepository roomRepository,
            PlayerRepository playerRepository,
            RoomCodeGenerator roomCodeGenerator,
            PlatformTransactionManager transactionManager
    ) {
        this(roomRepository, playerRepository, roomCodeGenerator, new TransactionTemplate(transactionManager));
    }

    CreateRoomService(
            RoomRepository roomRepository,
            PlayerRepository playerRepository,
            RoomCodeGenerator roomCodeGenerator,
            TransactionOperations transactionOperations
    ) {
        this.roomRepository = roomRepository;
        this.playerRepository = playerRepository;
        this.roomCodeGenerator = roomCodeGenerator;
        this.transactionOperations = transactionOperations;
    }

    public CreateRoomResult createRoom(CreateRoomCommand command) {
        String playerName = normalizePlayerName(command);

        // TODO: initializing with null looks dirty
        DataIntegrityViolationException lastFailure = null;
        for (int attempt = 0; attempt < MAX_ROOM_CREATION_ATTEMPTS; attempt++) {
            try {
                return transactionOperations.execute(status -> createRoomInTransaction(playerName));
            } catch (DataIntegrityViolationException exception) {
                lastFailure = exception;
            }
        }

        throw new RoomCodeUnavailableException(lastFailure);
    }

    private CreateRoomResult createRoomInTransaction(String playerName) {
        RoomEntity room = new RoomEntity();
        room.setCode(roomCodeGenerator.generateUniqueCode());
        room.setStatus(RoomStatus.LOBBY);

        RoomEntity savedRoom = roomRepository.save(room);
        roomRepository.flush();

        PlayerEntity hostPlayer = new PlayerEntity();
        hostPlayer.setRoomId(savedRoom.getId());
        hostPlayer.setNickname(playerName);
        hostPlayer.setHost(true);
        hostPlayer.setConnectionStatus(PlayerConnectionStatus.CONNECTED);

        PlayerEntity savedHostPlayer = playerRepository.save(hostPlayer);
        savedRoom.setHostPlayerId(savedHostPlayer.getId());
        roomRepository.save(savedRoom);

        return new CreateRoomResult(savedRoom.getCode(), savedRoom.getId(), savedHostPlayer.getId(), savedHostPlayer.getNickname());
    }

    // TODO: resolve: duplicate #1 A
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
