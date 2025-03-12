package eightplusone.bit.fit.domain.auth.filter;

import static eightplusone.bit.fit.global.constants.TokenConstant.*;

import java.io.IOException;
import java.util.Date;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.GenericFilterBean;

import com.fasterxml.jackson.databind.ObjectMapper;

import eightplusone.bit.fit.domain.auth.jwt.TokenProvider;
import eightplusone.bit.fit.global.dto.ResponseDto;
import eightplusone.bit.fit.global.utils.CookieUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomLogoutFilter extends GenericFilterBean {

	private final TokenProvider tokenProvider;
	private final ObjectMapper objectMapper;

	public CustomLogoutFilter(TokenProvider tokenProvider, ObjectMapper objectMapper) {
		this.tokenProvider = tokenProvider;
		this.objectMapper = objectMapper;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws
		IOException,
		ServletException {
		doFilter((HttpServletRequest)request, (HttpServletResponse)response, chain);
	}

	private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws
		IOException,
		ServletException {
		if (!request.getRequestURI().equals("/api/v1/auth/logout") || !request.getMethod().equals("POST")) {
			filterChain.doFilter(request, response);
			return;
		}

		String accessToken = tokenProvider.resolveAccessToken(request);
		Claims claimsByAccessToken = tokenProvider.getClaimsByAccessToken(accessToken);

		String email = claimsByAccessToken.getSubject();
		String role = claimsByAccessToken.get(AUTHORITIES_KEY).toString();

		tokenProvider.invalidateRefreshToken(email);

		log.info("{}-{}: logout ({})", email, role, new Date());
		response.addHeader(HttpHeaders.SET_COOKIE,
			CookieUtil.createCookie(REFRESH_TOKEN_COOKIE_NAME, null, REFRESH_EXPIRATION_DELETE).toString());
		response.setStatus(HttpServletResponse.SC_OK);
		response.setCharacterEncoding("utf-8");
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter()
			.write(objectMapper.writeValueAsString(ResponseDto.success(HttpStatus.OK, "로그아웃 성공", null)));
	}
}

