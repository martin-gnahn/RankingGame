package com.example.rankinggame.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    public static final String WEBSOCKET_ENDPOINT = "/ws";
    public static final String TOPIC_PREFIX = "/topic";
    public static final String APPLICATION_DESTINATION_PREFIX = "/app";
    public static final String LOCAL_FRONTEND_ORIGIN = "http://localhost:4200";
    public static final String RAILWAY_FRONTEND_ORIGIN = "https://martin-gnahn.dev";
    public static final String WWW_RAILWAY_FRONTEND_ORIGIN = "https://www.martin-gnahn.dev";

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(WEBSOCKET_ENDPOINT)
                .setAllowedOrigins(LOCAL_FRONTEND_ORIGIN, RAILWAY_FRONTEND_ORIGIN, WWW_RAILWAY_FRONTEND_ORIGIN);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker(TOPIC_PREFIX);
        registry.setApplicationDestinationPrefixes(APPLICATION_DESTINATION_PREFIX);
    }
}
