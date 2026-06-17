package com.example.rankinggame.config;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketConfigTest {
    private final WebSocketConfig config = new WebSocketConfig();

    @Test
    void registersStompEndpointWithAllowedFrontendCors() {
        StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
        StompWebSocketEndpointRegistration registration = mock(StompWebSocketEndpointRegistration.class);
        when(registry.addEndpoint(WebSocketConfig.WEBSOCKET_ENDPOINT)).thenReturn(registration);

        config.registerStompEndpoints(registry);

        verify(registry).addEndpoint(WebSocketConfig.WEBSOCKET_ENDPOINT);
        verify(registration).setAllowedOrigins(
                WebSocketConfig.LOCAL_FRONTEND_ORIGIN,
                WebSocketConfig.RAILWAY_FRONTEND_ORIGIN,
                WebSocketConfig.WWW_RAILWAY_FRONTEND_ORIGIN
        );
    }

    @Test
    void configuresTopicBrokerAndApplicationDestinationPrefix() {
        TestMessageBrokerRegistry registry = new TestMessageBrokerRegistry(
                mock(SubscribableChannel.class),
                mock(MessageChannel.class)
        );

        config.configureMessageBroker(registry);

        assertThat(registry.applicationDestinationPrefixes())
                .containsExactly(WebSocketConfig.APPLICATION_DESTINATION_PREFIX);
        assertThat(registry.simpleBrokerDestinationPrefixes())
                .containsExactly(WebSocketConfig.TOPIC_PREFIX);
    }

    private static class TestMessageBrokerRegistry extends MessageBrokerRegistry {
        private final SubscribableChannel clientInboundChannel;

        TestMessageBrokerRegistry(SubscribableChannel clientInboundChannel, MessageChannel clientOutboundChannel) {
            super(clientInboundChannel, clientOutboundChannel);
            this.clientInboundChannel = clientInboundChannel;
        }

        Collection<String> applicationDestinationPrefixes() {
            return getApplicationDestinationPrefixes();
        }

        Collection<String> simpleBrokerDestinationPrefixes() {
            SimpleBrokerMessageHandler simpleBroker = getSimpleBroker(clientInboundChannel);
            return simpleBroker.getDestinationPrefixes();
        }
    }
}
