package eightplusone.bit.fit.domain.auth.controller;

import static eightplusone.bit.fit.global.constants.TokenConstant.*;
import static eightplusone.bit.fit.global.utils.CookieUtil.*;
import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.HttpStatus.*;

import java.util.Date;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eightplusone.bit.fit.domain.auth.jwt.TokenProvider;
import eightplusone.bit.fit.global.utils.CookieUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final TokenProvider tokenProvider;

	@PostMapping("/reissue")
	public ResponseEntity<?> reissue(HttpServletRequest request) {
		String accessToken = tokenProvider.resolveAccessToken(request);
		Claims claimsByAccessToken = tokenProvider.getClaimsByAccessToken(accessToken);

		String role = claimsByAccessToken.get(AUTHORITIES_KEY).toString();
		String subject = claimsByAccessToken.getSubject();
		String requestRefreshToken = CookieUtil.findCookieByName(request, REFRESH_TOKEN_COOKIE_NAME).getValue();

		boolean isValid = tokenProvider.validateRefreshTokenWithAccessTokenInfo(role, subject, requestRefreshToken);
		if (!isValid) {
			return ResponseEntity.status(UNAUTHORIZED)
				.header(
					HttpHeaders.SET_COOKIE,
					createCookie(REFRESH_TOKEN_COOKIE_NAME, null, REFRESH_EXPIRATION_DELETE).toString())
				.body(null);
		}

		String newAccessToken = tokenProvider.createAccessToken(subject, role, new Date());
		return ResponseEntity.status(OK)
			.header(AUTHORIZATION, newAccessToken)
			.body(null);
	}

	@PostMapping("/token-exchange")
	public ResponseEntity<?> exchange(HttpServletRequest request) {
		String accessToken = CookieUtil.findCookieByName(request, ACCESS_TOKEN_COOKIE_NAME).getValue();
		return ResponseEntity.status(OK)
			.header(AUTHORIZATION, BEARER_PREFIX + accessToken)
			.header(SET_COOKIE, createCookie(ACCESS_TOKEN_COOKIE_NAME, null, REFRESH_EXPIRATION_DELETE).toString())
			.body(null);
	}
}
