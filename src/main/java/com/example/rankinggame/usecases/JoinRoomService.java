package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.JoinRoomCommand;
import com.example.rankinggame.dto.JoinRoomResult;
import com.example.rankinggame.entities.PlayerConnectionStatus;
import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.entities.RoomEntity;
import com.example.rankinggame.entities.RoomStatus;
import com.example.rankinggame.events.PlayerJoinedRoomEvent;
import com.example.rankinggame.exceptions.RoomNotFoundException;
import com.example.rankinggame.repositories.PlayerRepository;
import com.example.rankinggame.repositories.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class JoinRoomService {
    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RoomCodeService roomCodeService;

    @Transactional
    public JoinRoomResult joinRoom(JoinRoomCommand command) {
        String roomCode = roomCodeService.normalizeRoomCode(command);
        String playerName = normalizePlayerName(command);

        RoomEntity room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RoomNotFoundException(roomCode));

        if (room.getStatus() != RoomStatus.LOBBY) {
            throw new IllegalArgumentException("Room is not open for joining");
        }

        boolean nicknameTaken = playerRepository.findByRoomId(room.getId()).stream()
                .anyMatch(player -> player.getNickname().equalsIgnoreCase(playerName));

        if (nicknameTaken) {
            throw new PlayerNameAlreadyTakenException();
        }

        PlayerEntity player = new PlayerEntity();
        player.setRoomId(room.getId());
        player.setNickname(playerName);
        player.setConnectionStatus(PlayerConnectionStatus.CONNECTED);

        PlayerEntity savedPlayer = savePlayerAndVerifyUniqueName(player);

        eventPublisher.publishEvent(new PlayerJoinedRoomEvent(
                room.getCode(),
                savedPlayer.getId(),
                savedPlayer.getNickname(),
                false
        ));

        return new JoinRoomResult(room.getCode(), room.getId(), savedPlayer.getId(), savedPlayer.getNickname());
    }

    private PlayerEntity savePlayerAndVerifyUniqueName(PlayerEntity player) {
        try {
            PlayerEntity savedPlayer = playerRepository.save(player);
            playerRepository.flush();
            return savedPlayer;
        } catch (DataIntegrityViolationException exception) {
            throw new PlayerNameAlreadyTakenException(exception);
        }
    }

    private String normalizePlayerName(JoinRoomCommand command) {
        return PlayerNameNormalizer.normalize(command == null ? null : command.playerName());
    }
}
