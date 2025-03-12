package eightplusone.bit.fit.domain.auth.jwt;

import static eightplusone.bit.fit.global.constants.TokenConstant.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpHeaders.*;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import eightplusone.bit.fit.domain.auth.dto.CustomUserDetails;
import eightplusone.bit.fit.domain.auth.service.CustomUserDetailsService;
import eightplusone.bit.fit.domain.auth.service.RedisTokenService;
import eightplusone.bit.fit.domain.user.entity.User;
import eightplusone.bit.fit.global.exception.CustomException;
import eightplusone.bit.fit.support.fixture.UserFixture;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class TokenProviderTest {
	private TokenProvider tokenProvider;

	private static final String TEST_ACCESS_SECRET = "accessabcdefghijklmnopqrstuvwxyz";
	private static final String TEST_REFRESH_SECRET = "refreshabcdefghijklmnopqrstuvwxyz";
	private static final long TEST_EXPIRATION = 3600;
	private static final String TEST_EMAIL = "test@gmail.com";
	private static final String TEST_ROLE_USER = "ROLE_USER";

	@Mock
	private RedisTokenService redisTokenService;

	@Mock
	private CustomUserDetailsService customUserDetailsService;

	@BeforeEach
	void setUp() {
		tokenProvider = new TokenProvider(
			redisTokenService,
			customUserDetailsService,
			TEST_ACCESS_SECRET,
			TEST_REFRESH_SECRET,
			TEST_EXPIRATION,
			TEST_EXPIRATION
		);
	}

	@Test
	@DisplayName("accessToken을 생성한다")
	void createAccessToken() {
		//when
		String accessToken = tokenProvider.createAccessToken(TEST_EMAIL, TEST_ROLE_USER, new Date());

		Claims claims = parseTokenSubject(accessToken, TEST_ACCESS_SECRET);
		String subject = claims.getSubject();
		String role = claims.get(AUTHORITIES_KEY).toString();

		//then
		assertAll(
			() -> assertThat(accessToken).isNotNull(),
			() -> assertThat(subject).isEqualTo(TEST_EMAIL),
			() -> assertThat(role).isEqualTo(TEST_ROLE_USER)
		);
	}

	@Test
	@DisplayName("refreshToken을 생성한다")
	void createRefreshToken() {
		//when
		String refreshToken = tokenProvider.createRefreshToken(TEST_EMAIL, new Date());
		String subject = parseTokenSubject(refreshToken, TEST_REFRESH_SECRET).getSubject();

		//then
		assertAll(
			() -> assertThat(refreshToken).isNotNull(),
			() -> assertThat(subject).isEqualTo(TEST_EMAIL)
		);
		verify(redisTokenService, times(1)).saveRefreshToken(TEST_EMAIL, refreshToken, TEST_EXPIRATION);
	}

	@Test
	@DisplayName("사용자 권한 정보를 accessToken으로 조회한다")
	void getAuthenticationByAccessToken_ValidToken_ReturnsAuthentication() {
		//given
		User user = UserFixture.USER_FIXTURE_1.createUser();

		String accessToken = tokenProvider.createAccessToken(user.getEmail(), user.getRole().getKey(), new Date());
		CustomUserDetails customUserDetails = new CustomUserDetails(user);
		when(customUserDetailsService.loadUserByUsername(user.getEmail())).thenReturn(customUserDetails);

		//when
		Authentication authentication = tokenProvider.getAuthenticationByAccessToken(accessToken);

		//then
		assertAll(
			() -> assertThat(authentication).isNotNull(),
			() -> assertThat(authentication.getName()).isEqualTo(user.getEmail()),
			() -> assertThat(authentication.getAuthorities())
				.hasSize(1)
				.extracting(authority -> authority.getAuthority())
				.containsExactly(user.getRole().getKey())
		);
	}

	@Test
	@DisplayName("accessToken으로 Claims를 조회한다")
	void getClaimsByAccessToken_ValidToken_ReturnsClaims() {
		//given
		String accessToken = tokenProvider.createAccessToken(TEST_EMAIL, TEST_ROLE_USER, new Date());

		//when
		Claims claims = tokenProvider.getClaimsByAccessToken(accessToken);
		String subject = claims.getSubject();
		String role = claims.get(AUTHORITIES_KEY).toString();

		//then
		assertAll(
			() -> assertThat(subject).isEqualTo(TEST_EMAIL),
			() -> assertThat(role).isEqualTo(TEST_ROLE_USER)
		);
	}

	@Test
	@DisplayName("refreshToken으로 Claims를 조회한다")
	void getClaimsByRefreshToken_ValidToken_ReturnsClaims() {
		//given
		String refreshToken = tokenProvider.createRefreshToken(TEST_EMAIL, new Date());

		//when
		Claims claims = tokenProvider.getClaimsRefreshToken(refreshToken);
		String subject = claims.getSubject();

		//then
		assertThat(subject).isEqualTo(TEST_EMAIL);
	}

	@Test
	@DisplayName("요청에서 accessToken을 추출한다")
	void resolveAccessToken_ValidRequest_ReturnsAccessToken() {
		//given
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader(AUTHORIZATION))
			.thenReturn(BEARER_PREFIX + "testAccessToken");

		//when
		String accessToken = tokenProvider.resolveAccessToken(request);

		//then
		assertThat(accessToken).isEqualTo("testAccessToken");
	}

	@Test
	@DisplayName("요청의_accessToken에_Bearer이_포함되지않으면_null을_반환한다")
	void resolveAccessToken_NoBearerPrefix_ReturnsNull() {
		//given
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader(AUTHORIZATION)).thenReturn("noBearer testAccessToken");

		// when
		String result = tokenProvider.resolveAccessToken(request);

		// then
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("요청의 accessToken이 존재하지않으면 null을 반환한다")
	void resolveAccessToken_NoAccessToken_ReturnsNull() {
		//given
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader(AUTHORIZATION)).thenReturn(null);

		// when
		String result = tokenProvider.resolveAccessToken(request);

		// then
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("accessToken의 유효성을 검증한다")
	void validateAccessToken_ValidToken_ReturnsTrue() {
		//given
		String accessToken = tokenProvider.createAccessToken(TEST_EMAIL, TEST_ROLE_USER, new Date());

		//when
		boolean isValid = tokenProvider.validateAccessToken(accessToken);

		//then
		assertThat(isValid).isTrue();
	}

	@Test
	@DisplayName("만료된 accessToekn을 검증한다")
	void validateAccessToken_ExpiredToken_ReturnsFalse() {
		// given
		String expiredAccessToken = tokenProvider.createAccessToken(TEST_EMAIL, TEST_ROLE_USER,
			new Date(System.currentTimeMillis() - TEST_EXPIRATION * 1000));
		// when
		boolean isValid = tokenProvider.validateAccessToken(expiredAccessToken);

		// then
		assertThat(isValid).isFalse();
	}

	@Test
	@DisplayName("refreshToken의 유효성을 검증한다")
	void validateRefreshToken_ValidToken_ReturnsTrue() {
		//given
		String refreshToken = tokenProvider.createRefreshToken(TEST_EMAIL, new Date());

		//when
		boolean isValid = tokenProvider.validateRefreshToken(refreshToken);

		//then
		assertThat(isValid).isTrue();
	}

	@Test
	@DisplayName("만료된 refreshToken을 검증한다")
	void validateRefreshToken_ExpiredToken_ReturnsFalse() {
		// given
		String expiredRefreshToken = tokenProvider.createRefreshToken(TEST_EMAIL,
			new Date(System.currentTimeMillis() - TEST_EXPIRATION * 1000));

		// when
		boolean isValid = tokenProvider.validateAccessToken(expiredRefreshToken);

		// then
		assertThat(isValid).isFalse();
	}

	@Test
	@DisplayName("재발급 토큰을 무효화한다")
	void invalidateRefreshToken_DeletesTokenFromRedis() {
		//when
		tokenProvider.invalidateRefreshToken(TEST_EMAIL);

		//then
		verify(redisTokenService, times(1))
			.deleteRefreshToken(TEST_EMAIL);
	}

	@Test
	@DisplayName("요청된 refreshToken이 Redis의 토큰과 일치한다")
	void validateRefreshTokenWithAccessTokenInfo_MatchingToken_ReturnsTrue() {
		//given
		String requestRefreshToken = "validRefreshToken";
		when(redisTokenService.getRefreshToken(TEST_EMAIL)).thenReturn(requestRefreshToken);

		//when
		boolean isValid = tokenProvider.validateRefreshTokenWithAccessTokenInfo(TEST_ROLE_USER, TEST_EMAIL,
			requestRefreshToken);

		//then
		assertThat(isValid).isTrue();
		verify(redisTokenService, times(1)).getRefreshToken(TEST_EMAIL);
	}

	@Test
	@DisplayName("요청된 refreshToken이 Redis의 토큰과 불일치한다")
	void validateRefreshTokenWithAccessTokenInfo_NonMatchingToken_ReturnsFalse() {
		//given
		String requestRefreshToken = "invalidRefreshToken";
		String redisRefreshToken = "refreshToken";
		when(redisTokenService.getRefreshToken(TEST_EMAIL)).thenReturn(redisRefreshToken);

		//when
		boolean isValid = tokenProvider.validateRefreshTokenWithAccessTokenInfo(TEST_ROLE_USER, TEST_EMAIL,
			requestRefreshToken
		);

		//then
		assertThat(isValid).isFalse();
		verify(redisTokenService, times(1)).getRefreshToken(TEST_EMAIL);
		verify(redisTokenService, times(1)).deleteRefreshToken(TEST_EMAIL);
	}

	@Test
	@DisplayName("요청된 accessToken정보에 맞는 refreshToken이 Redis에 없다")
	void validateRefreshTokenWithAccessTokenInfo_NoTokenInRedis_ReturnsFalse() {
		//given
		String requestRefreshToken = "anyRefreshToken";
		when(redisTokenService.getRefreshToken(TEST_EMAIL)).thenReturn(null);

		//when
		boolean isValid = tokenProvider.validateRefreshTokenWithAccessTokenInfo(TEST_ROLE_USER, TEST_EMAIL,
			requestRefreshToken
		);

		//then
		assertThat(isValid).isFalse();
		verify(redisTokenService, times(1)).getRefreshToken(TEST_EMAIL);
		verify(redisTokenService, times(1)).deleteRefreshToken(TEST_EMAIL);
	}

	@Test
	@DisplayName("지정된 accessToken 만료시간을 조회한다")
	void getAccessTokenExpirationSeconds_ReturnsConfiguredExpiration() {
		//when
		long accessTokenExpirationSeconds = tokenProvider.getAccessTokenExpirationSeconds();

		//then
		assertThat(accessTokenExpirationSeconds).isEqualTo(TEST_EXPIRATION);

	}

	@Test
	@DisplayName("지정된 refreshToken 만료시간을 조회한다")
	void getRefreshTokenExpirationSeconds_ReturnsConfiguredExpiration() {
		//when
		long refreshTokenExpirationSeconds = tokenProvider.getRefreshTokenExpirationSeconds();

		//then
		assertThat(refreshTokenExpirationSeconds).isEqualTo(TEST_EXPIRATION);
	}

	@Test
	@DisplayName("만료된 토큰을 파싱하려고 하면_만료된 Claims를 반환한다")
	void getClaimsByAccessToken_ExpiredToken_ReturnsExpiredClaims() {
		// given
		String expiredToken = tokenProvider.createAccessToken(TEST_EMAIL, TEST_ROLE_USER,
			new Date(System.currentTimeMillis() - TEST_EXPIRATION * 1000));

		// when
		Claims claims = tokenProvider.getClaimsByAccessToken(expiredToken);

		// then
		assertThat(claims).isNotNull();
	}

	@Test
	@DisplayName("잘못된 서명의 토큰을 검증하면 false를 반환한다")
	void validateAccessToken_InvalidSignature_ReturnsFalse() {
		// given
		String accessToken = tokenProvider.createAccessToken(TEST_EMAIL, TEST_ROLE_USER, new Date());
		String invalidSignature = "invalidSignature";
		accessToken += invalidSignature;

		// when
		boolean isValid = tokenProvider.validateAccessToken(accessToken);

		// then
		assertThat(isValid).isFalse();
	}

	@Test
	@DisplayName("잘못된 형식의 토큰을 검증하면 false를 반환한다")
	void validateAccessToken_MalformedToken_ReturnsFalse() {
		// given
		String malformedToken = "malformedToken";

		// when
		boolean isValid = tokenProvider.validateAccessToken(malformedToken);

		// then
		assertThat(isValid).isFalse();
	}

	@Test
	@DisplayName("지원되지 않는 토큰을 검증하면 false를 반환한다")
	void validateAccessToken_UnsupportedToken_ReturnsFalse() {
		// given
		String unsupportedToken = tokenProvider.createAccessToken(TEST_EMAIL, TEST_ROLE_USER, new Date());

		// when
		boolean isValid = tokenProvider.validateAccessToken(unsupportedToken.replace(".", "/"));

		// then
		assertThat(isValid).isFalse();
	}

	@Test
	@DisplayName("토큰이 null이면 false를 반환한다")
	void validateAccessToken_NullToken_ReturnsFalse() {
		// given
		String nullToken = null;

		// when
		boolean isValid = tokenProvider.validateAccessToken(nullToken);

		// then
		assertThat(isValid).isFalse();
	}

	@Test
	@DisplayName("비어있는 토큰을 검증하면 false를 반환한다")
	void validateAccessToken_EmptyToken_ReturnsFalse() {
		// given
		String emptyToken = "";

		// when
		boolean isValid = tokenProvider.validateAccessToken(emptyToken);

		// then
		assertThat(isValid).isFalse();
	}

	@Test
	@DisplayName("비어있는 Claims 토큰을_검증하면 false를 반환한다")
	void validateAccessToken_EmptyClaims_ReturnsFalse() {
		// given
		String emptyClaimToken = Jwts.builder()
			.signWith(Keys.hmacShaKeyFor(TEST_ACCESS_SECRET.getBytes()))
			.compact();

		// when
		boolean isValid = tokenProvider.validateAccessToken(emptyClaimToken);

		// then
		assertThat(isValid).isFalse();
	}

	@Test
	@DisplayName("잘못된 토큰이면 에러가 발생한다")
	void getClaimsByAccessToken_InvalidToken_ThrowsException() {
		// given
		String invalidToken = "invalidToken";

		// when & then
		assertThatThrownBy(() -> tokenProvider.getClaimsByAccessToken(invalidToken))
			.isInstanceOf(CustomException.class);
	}

	private Claims parseTokenSubject(String token, String secretKey) {
		return Jwts.parser()
			.verifyWith(Keys.hmacShaKeyFor(secretKey.getBytes()))
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}
}
