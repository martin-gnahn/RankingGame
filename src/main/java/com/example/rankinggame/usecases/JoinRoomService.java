package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.JoinRoomCommand;
import com.example.rankinggame.dto.JoinRoomResult;
import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.entities.PlayerConnectionStatus;
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
    private static final int MAX_PLAYER_NAME_LENGTH = 80;

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
            throw new IllegalArgumentException("Player name is already taken");
        }

        PlayerEntity player = new PlayerEntity();
        player.setRoomId(room.getId());
        player.setNickname(playerName);
        player.setHost(false);
        player.setConnectionStatus(PlayerConnectionStatus.CONNECTED);

        PlayerEntity savedPlayer;
        try {
            savedPlayer = playerRepository.save(player);
            // TODO: what does this flush? It looks dirty.
            playerRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            // TODO: using this exception looks dirty. Prefer custom exception with no hardcoded error message. Same as for errors above.
            throw new IllegalArgumentException("Player name is already taken", exception);
        }

        eventPublisher.publishEvent(new PlayerJoinedRoomEvent(
                room.getCode(),
                savedPlayer.getId(),
                savedPlayer.getNickname(),
                false
        ));

        return new JoinRoomResult(room.getCode(), room.getId(), savedPlayer.getId(), savedPlayer.getNickname());
    }

    // TODO: duplicate #1 B
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
