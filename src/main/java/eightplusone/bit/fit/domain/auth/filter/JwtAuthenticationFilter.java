package eightplusone.bit.fit.domain.auth.filter;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import eightplusone.bit.fit.domain.auth.jwt.TokenProvider;
import eightplusone.bit.fit.global.dto.ResponseDto;
import eightplusone.bit.fit.global.enums.ApiEndpoint;
import eightplusone.bit.fit.global.exception.CustomException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final TokenProvider tokenProvider;
	private final ObjectMapper objectMapper;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {

		/* OAuth2 재로그인 무한 루프 방지*/
		String requestUri = request.getRequestURI();
		if (requestUri.matches("^\\/login(?:\\/.*)?$")) {
			filterChain.doFilter(request, response);
			return;
		}
		if (requestUri.matches("^\\/oauth2(?:\\/.*)?$")) {
			filterChain.doFilter(request, response);
			return;
		}

		String accessToken = tokenProvider.resolveAccessToken(request);

		try {
			if (!tokenProvider.validateAccessToken(accessToken)) {
				if (isOptionalAuthPath(requestUri)) { // 선택적 필터 적용 조건문
					filterChain.doFilter(request, response);
					return;
				}
				sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰 입니다.");
				return;
			}
			Authentication authentication = tokenProvider.getAuthenticationByAccessToken(accessToken);
			SecurityContextHolder.getContext().setAuthentication(authentication);

		} catch (CustomException e) {
			SecurityContextHolder.clearContext();
			sendErrorResponse(response, HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
			return;
		}
		filterChain.doFilter(request, response);
	}

	private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
		response.setStatus(status.value());
		response.setCharacterEncoding("utf-8");
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write(objectMapper.writeValueAsString(
			ResponseDto.fail(status, message)));
	}

	protected boolean shouldNotFilter(HttpServletRequest request) {
		String requestUri = request.getRequestURI();
		String upgradeHeader = request.getHeader("Upgrade");

		if (requestUri.startsWith("/api/v1/session")) {
			return false;
		}

		return Arrays.stream(ApiEndpoint.values())
			.filter(endpoint -> endpoint.name().startsWith("PUBLIC_"))
			.flatMap(endpoint -> Arrays.stream(endpoint.getPaths()))
			.map(path -> path.replace("**", ".*"))
			.anyMatch(requestUri::matches)
			|| (upgradeHeader != null && upgradeHeader.equalsIgnoreCase("websocket")
		);
	}

	private boolean isOptionalAuthPath(String requestUri) {
		return requestUri.startsWith("/api/v1/session") &&
			!requestUri.equals("/api/v1/session/recommended");
	}
}
