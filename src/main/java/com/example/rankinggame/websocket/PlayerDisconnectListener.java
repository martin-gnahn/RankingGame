package com.example.rankinggame.websocket;

import com.example.rankinggame.entities.Player;
import com.example.rankinggame.entities.PlayerConnectionStatus;
import com.example.rankinggame.events.PlayerLeftRoomEvent;
import com.example.rankinggame.repositories.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@RequiredArgsConstructor
@Component
public class PlayerDisconnectListener {
    private final LivePlayerSessionRegistry sessionRegistry;
    private final PlayerRepository playerRepository;
    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    @Transactional
    public void handleDisconnect(SessionDisconnectEvent event) {
        sessionRegistry.remove(event.getSessionId())
                .flatMap(session -> playerRepository.findById(session.playerId())
                        .map(player -> disconnectPlayer(session, player)));
    }

    private Player disconnectPlayer(LivePlayerSession session, Player player) {
        player.setConnectionStatus(PlayerConnectionStatus.DISCONNECTED);
        Player savedPlayer = playerRepository.save(player);
        eventPublisher.publishEvent(new PlayerLeftRoomEvent(session.roomCode(), savedPlayer.getId()));
        return savedPlayer;
    }
}
