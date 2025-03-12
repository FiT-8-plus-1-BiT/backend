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
import eightplusone.bit.fit.global.dto.ResponseDto;
import eightplusone.bit.fit.global.utils.CookieUtil;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final TokenProvider tokenProvider;

	@Operation(summary = "재발급 요청", description = "**성공 데이터:** 헤더의 `토큰`"
		+ "무결성 침해 토큰으로 간주 시 `Refresh Token 초기화 진행 후 재로그인`을 유도합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "재발급 성공"),
		@ApiResponse(responseCode = "401", description = "재발급 실패, 무결성이 침해되었습니다. 재 로그인이 필요합니다."),
	})
	@PostMapping("/reissue")
	public ResponseEntity<ResponseDto<Object>> reissue(HttpServletRequest request) {
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
				.body(ResponseDto.fail(UNAUTHORIZED, "재발급 실패, 무결성이 침해되었습니다. 재 로그인이 필요합니다."));
		}

		String newAccessToken = tokenProvider.createAccessToken(subject, role, new Date());
		return ResponseEntity.status(OK)
			.header(AUTHORIZATION, newAccessToken)
			.body(ResponseDto.success(OK, "재발급 성공", null));
	}

	@Operation(summary = "쿠키의 토큰 헤더로 변환하는 요청", description = "**성공 데이터:** 헤더의 `토큰`"
		+ "소셜 로그인 성공 이후 302 리다이렉트로 받은 쿠키의 accessToken을 헤더로 재전송합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "변환 성공"),
		@ApiResponse(responseCode = "401", description = "변환 실패"),
	})
	@PostMapping("/token-exchange")
	public ResponseEntity<ResponseDto<Object>> exchange(HttpServletRequest request) {
		String accessToken = CookieUtil.findCookieByName(request, ACCESS_TOKEN_COOKIE_NAME).getValue();
		return ResponseEntity.status(OK)
			.header(AUTHORIZATION, BEARER_PREFIX + accessToken)
			.header(SET_COOKIE, createCookie(ACCESS_TOKEN_COOKIE_NAME, null, REFRESH_EXPIRATION_DELETE).toString())
			.body(ResponseDto.success(OK, "변환 성공", null));
	}

	@Operation(summary = "로그아웃 요청", description = "**성공 응답 데이터:**  브라우저 쿠키 초기화")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "201", description = "로그아웃 성공"),
		@ApiResponse(responseCode = "401", description = "로그아웃 실패"),
	})
	@PostMapping("/logout")
	public ResponseEntity<ResponseDto<Object>> logout() {
		return ResponseEntity.status(OK).body(ResponseDto.success(OK, "로그아웃 성공", null));
	}
}
