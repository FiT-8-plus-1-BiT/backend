package eightplusone.bit.fit.global.config;

import org.kurento.client.KurentoClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import eightplusone.bit.fit.domain.auth.jwt.TokenProvider;
import eightplusone.bit.fit.domain.streaming.RoomManager;
import eightplusone.bit.fit.global.websocket.CallHandler;
import lombok.RequiredArgsConstructor;

@EnableWebSocket
@Configuration
@RequiredArgsConstructor
public class AudioWebSocketConfig implements WebSocketConfigurer {

	private final TokenProvider tokenProvider;
	@Value("${spring.kurento.ws-url}")
	private String kurentoWsUrl;

	@Bean
	public RoomManager roomManager() {
		return new RoomManager();
	}

	@Bean
	public CallHandler callHandler(RoomManager roomManager) {
		return new CallHandler(roomManager, tokenProvider);
	}

	@Bean
	public KurentoClient kurentoClient() {
		return KurentoClient.create(kurentoWsUrl);
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(callHandler(roomManager()), "/call").setAllowedOrigins("*");
	}

}
