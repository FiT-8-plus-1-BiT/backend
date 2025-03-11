package eightplusone.bit.fit.domain.auth.filter;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

import eightplusone.bit.fit.domain.auth.jwt.TokenProvider;
import eightplusone.bit.fit.global.enums.ApiEndpoint;
import io.jsonwebtoken.IncorrectClaimException;
import io.jsonwebtoken.JwtException;
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
				throw new JwtException("잘못된 토큰 입니다.");
			}
			Authentication authentication = tokenProvider.getAuthenticationByAccessToken(accessToken);
			SecurityContextHolder.getContext().setAuthentication(authentication);

		} catch (IncorrectClaimException e) {
			SecurityContextHolder.clearContext();
			log.debug("잘못된 토큰 입니다.");
			throw new JwtException("잘못된 토큰 입니다.", e);
		} catch (UsernameNotFoundException e) {
			SecurityContextHolder.clearContext();
			log.debug("회원을 찾을 수 없습니다.");
			throw new UsernameNotFoundException("회원을 찾을 수 없습니다.");
		}
		filterChain.doFilter(request, response);
	}

	protected boolean shouldNotFilter(HttpServletRequest request) {
		String requestUri = request.getRequestURI();
		String upgradeHeader = request.getHeader("Upgrade");
		String requestUrl = request.getRequestURL().toString();

		return Arrays.stream(ApiEndpoint.values())
			.filter(endpoint -> endpoint.name().startsWith("PUBLIC_"))
			.flatMap(endpoint -> Arrays.stream(endpoint.getPaths()))
			.map(path -> path.replace("**", ".*"))
			.anyMatch(requestUri::matches)
			|| (upgradeHeader != null && upgradeHeader.equalsIgnoreCase("websocket")
			|| requestUri.startsWith("/ws-room")
			|| requestUri.startsWith("/sub/session")
			|| requestUrl.startsWith("https://jiangxy.github.io") // TODO : 마지막 조건문 삭제
		);
	}
}
