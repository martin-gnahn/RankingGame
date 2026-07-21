package com.example.rankinggame.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
class PlayerPresenceSchedulerConfig {
    @Bean(destroyMethod = "shutdown")
    ScheduledExecutorService playerPresenceDisconnectExecutor() {
        return Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "player-presence-disconnect");
            thread.setDaemon(true);
            return thread;
        });
    }
}
