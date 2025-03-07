package eightplusone.bit.fit.domain.auth.controller;

import static eightplusone.bit.fit.global.constants.TokenConstant.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpHeaders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Date;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;

import eightplusone.bit.fit.domain.auth.jwt.TokenProvider;
import eightplusone.bit.fit.domain.auth.service.CustomOAuth2UserService;
import eightplusone.bit.fit.global.config.SecurityConfig;
import eightplusone.bit.fit.support.annotation.WithMockCustom;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@WebMvcTest(controllers = AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

	private static final String ROLE_USER = "ROLE_USER";
	private static final String API_AUTH_BASE_URL = "/api/v1/auth";
	private static final String TEST_SUBJECT = "test@gmail.com";
	private static final String TEST_ACCESS_TOKEN = "testAccessToken";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	CustomOAuth2UserService customOAuth2UserService;

	@MockitoBean
	TokenProvider tokenProvider;

	@Autowired
	ObjectMapper objectMapper;

	@Test
	@WithMockCustom(role = ROLE_USER)
	@DisplayName("로그아웃 요청 테스트")
	void userLogout() throws Exception {
		//given
		Claims claims = Mockito.mock(Claims.class);
		Mockito.when(tokenProvider.resolveAccessToken(Mockito.any(HttpServletRequest.class)))
			.thenReturn(TEST_ACCESS_TOKEN);
		Mockito.when(tokenProvider.getClaimsByAccessToken(TEST_ACCESS_TOKEN))
			.thenReturn(claims);
		Mockito.when(claims.getSubject()).thenReturn(TEST_SUBJECT);
		Mockito.when(claims.get(AUTHORITIES_KEY)).thenReturn(ROLE_USER);
		Mockito.when(tokenProvider.validateAccessToken(TEST_ACCESS_TOKEN)).thenReturn(true);

		//when
		ResultActions actions = mockMvc.perform(
			post(API_AUTH_BASE_URL + "/logout")
				.header(AUTHORIZATION, BEARER_PREFIX + TEST_ACCESS_TOKEN));

		//then
		actions
			.andExpect(status().isOk())
			.andExpect(header().exists(SET_COOKIE))
			.andExpect(header().string(SET_COOKIE, containsString(REFRESH_TOKEN_COOKIE_NAME + "=;")))
			.andExpect(header().string(SET_COOKIE, containsString("Path=/")))
			.andExpect(header().string(SET_COOKIE, containsString("Max-Age=0")))
			.andExpect(header().string(SET_COOKIE, containsString("HttpOnly")))
			.andExpect(header().string(SET_COOKIE, containsString("SameSite=Strict")))
			.andDo(print());
	}

	@Test
	@DisplayName("토큰 재발급 요청 테스트")
	void reissueToken() throws Exception {
		// given
		String requestRefreshToken = "testRefreshToken";

		Claims claims = Mockito.mock(Claims.class);
		Mockito.when(tokenProvider.resolveAccessToken(Mockito.any(HttpServletRequest.class)))
			.thenReturn(TEST_ACCESS_TOKEN);
		Mockito.when(tokenProvider.getClaimsByAccessToken(TEST_ACCESS_TOKEN)).thenReturn(claims);
		Mockito.when(claims.getSubject()).thenReturn(TEST_SUBJECT);
		Mockito.when(claims.get(AUTHORITIES_KEY)).thenReturn(ROLE_USER);
		when(tokenProvider.validateRefreshTokenWithAccessTokenInfo(ROLE_USER, TEST_SUBJECT,
			requestRefreshToken)).thenReturn(true);

		String newAccessToken = "newAccessToken";
		Mockito.when(tokenProvider.createAccessToken(eq(TEST_SUBJECT), eq(ROLE_USER), any(Date.class)))
			.thenReturn(newAccessToken);

		// when
		ResultActions actions = mockMvc.perform(
			post(API_AUTH_BASE_URL + "/reissue")
				.header(AUTHORIZATION, BEARER_PREFIX + TEST_ACCESS_TOKEN)
				.cookie(new Cookie(REFRESH_TOKEN_COOKIE_NAME, requestRefreshToken)));

		// then
		actions
			.andExpect(status().isOk())
			.andExpect(header().string(AUTHORIZATION, newAccessToken))
			.andDo(print());
	}

	@Test
	@DisplayName("리다이렉트 토큰 쿠키 헤더 변환 요청 테스트")
	void exchangeToken() throws Exception {
		Cookie accessTokenCookie = new Cookie(ACCESS_TOKEN_COOKIE_NAME, TEST_ACCESS_TOKEN);

		// when
		ResultActions actions = mockMvc.perform(
			post(API_AUTH_BASE_URL + "/token-exchange")
				.cookie(accessTokenCookie)
		);

		// then
		actions
			.andExpect(status().isOk())
			.andExpect(header().string(AUTHORIZATION, BEARER_PREFIX + TEST_ACCESS_TOKEN))
			.andExpect(header().string(SET_COOKIE, containsString(ACCESS_TOKEN_COOKIE_NAME + "=;")))
			.andExpect(header().string(SET_COOKIE, containsString("Path=/")))
			.andExpect(header().string(SET_COOKIE, containsString("Max-Age=0")))
			.andExpect(header().string(SET_COOKIE, containsString("HttpOnly")))
			.andExpect(header().string(SET_COOKIE, containsString("SameSite=Strict")))
			.andDo(print());
	}
}
