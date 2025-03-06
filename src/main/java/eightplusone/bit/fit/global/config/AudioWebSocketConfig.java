package eightplusone.bit.fit.global.config;

import org.kurento.client.KurentoClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import eightplusone.bit.fit.domain.streaming.RoomManager;
import eightplusone.bit.fit.global.websocket.CallHandler;

@EnableWebSocket
@Configuration
public class AudioWebSocketConfig implements WebSocketConfigurer {
	@Bean
	public RoomManager roomManager() {
		return new RoomManager();
	}

	@Bean
	public CallHandler callHandler(RoomManager roomManager) {
		return new CallHandler(roomManager);
	}

	@Bean
	public KurentoClient kurentoClient() {
		return KurentoClient.create("ws://localhost:8888/kurento");
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(callHandler(roomManager()), "/call").setAllowedOrigins("*");
	}

}
