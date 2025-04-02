package eightplusone.bit.fit.domain.auth.interceptor;

import static eightplusone.bit.fit.global.constants.TokenConstant.*;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;

import eightplusone.bit.fit.domain.auth.jwt.TokenProvider;
import eightplusone.bit.fit.global.exception.CustomException;
import eightplusone.bit.fit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class WebSocketInterceptor implements ChannelInterceptor {

	private final TokenProvider tokenProvider;

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

		if (accessor == null) {
			return message;
		}

		StompCommand command = accessor.getCommand();

		if (command == StompCommand.CONNECT) {
			return message;
		}

		if (command == StompCommand.SUBSCRIBE || command == StompCommand.SEND) {
			String destination = accessor.getDestination();

			if (destination != null && destination.startsWith("/sub/session")) {
				return message;
			}

			String authHeader = accessor.getFirstNativeHeader("Authorization");
			if (authHeader == null) {
				authHeader = accessor.getFirstNativeHeader("authorization");
			}

			if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
				log.warn("잘못된 webSocket 토큰 헤더 값: {}", authHeader);
				throw new CustomException(ErrorCode.INVALID_AUTH_TOKEN);
			}

			String accessToken = authHeader.substring(BEARER_PREFIX.length());

			if (!tokenProvider.validateAccessToken(accessToken)) {
				log.warn("webSocket 토큰 유효성 실패");
				throw new CustomException(ErrorCode.INVALID_AUTH_TOKEN);
			}

			Authentication authentication = tokenProvider.getAuthenticationByAccessToken(accessToken);
			accessor.setUser(authentication);
		}

		return message;
	}
}
