package eightplusone.bit.fit.domain.auth.interceptor;

import static eightplusone.bit.fit.global.constants.TokenConstant.*;

import eightplusone.bit.fit.domain.auth.jwt.TokenProvider;
import eightplusone.bit.fit.global.exception.CustomException;
import eightplusone.bit.fit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class WebSocketInterceptor implements ChannelInterceptor {

	private final TokenProvider tokenProvider;

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

		// accessor가 null일 경우 바로 통과
		if (accessor == null) {
			return message;
		}

		StompCommand command = accessor.getCommand();

		if (command == StompCommand.CONNECT) {
			log.debug("🔗 WebSocket CONNECT 요청 허용");
			return message;
		}

		if (command == StompCommand.SUBSCRIBE || command == StompCommand.SEND) {
			String destination = accessor.getDestination();
			log.debug("📩 STOMP {} 요청 - dest: {}", command, destination);

			// 특정 경로 예외 처리
			if (destination != null && destination.startsWith("/sub/session")) {
				return message;
			}

			String authHeader = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);

			// 토큰이 없거나 형식이 잘못된 경우 예외 처리
			if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
				log.warn("❌ WebSocket 토큰 누락 또는 형식 오류");
				throw new CustomException(ErrorCode.INVALID_AUTH_TOKEN);
			}

			String accessToken = authHeader.substring(BEARER_PREFIX.length());

			if (!tokenProvider.validateAccessToken(accessToken)) {
				log.warn("❌ WebSocket 토큰 유효성 실패");
				throw new CustomException(ErrorCode.INVALID_AUTH_TOKEN);
			}

			Authentication authentication = tokenProvider.getAuthenticationByAccessToken(accessToken);
			accessor.setUser(authentication);
		}

		return message;
	}
}
