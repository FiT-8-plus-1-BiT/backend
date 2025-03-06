package eightplusone.bit.fit.domain.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisTokenServiceTest {

	@Mock
	private RedisTemplate<String, String> redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@InjectMocks
	private RedisTokenService redisTokenService;

	private static final String EMAIL = "test@gmail.com";
	private static final String PROVIDER = "GOOGLE_1234";

	private static final String REFRESH_TOKEN = "testRefreshToken";
	private static final String OAUTH2_ACCESS_TOKEN = "testOAuth2AccessToken";

	private static final String REFRESH_TOKEN_KEY = "RT:test@gmail.com";
	private static final String OAUTH2_ACCESS_TOKEN_KEY = "AT:GOOGLE_1234";

	private static final long EXPIRATION_SECONDS = 1000L * 60 * 60 * 24;

	@Test
	@DisplayName("RefreshToken을 Redis에 저장한다")
	void saveRefreshToken() {
		// given
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);

		// when
		redisTokenService.saveRefreshToken(EMAIL, REFRESH_TOKEN, EXPIRATION_SECONDS);

		// then
		verify(valueOperations, times(1))
			.set(REFRESH_TOKEN_KEY, REFRESH_TOKEN, EXPIRATION_SECONDS, TimeUnit.SECONDS);
	}

	@Test
	@DisplayName("Key에 해당하는 RefreshToken을 Redis에서_조회한다")
	void findRefreshToken() {
		// given
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(REFRESH_TOKEN_KEY)).thenReturn(REFRESH_TOKEN);

		// when
		String resultRefreshToken = redisTokenService.getRefreshToken(EMAIL);

		// then
		assertThat(resultRefreshToken).isEqualTo(REFRESH_TOKEN);
		verify(valueOperations, times(1)).get(REFRESH_TOKEN_KEY);
	}

	@Test
	@DisplayName("존재하지 않는 Key로 RefreshToken을 Redis에서 조회시 null을 반환한다")
	void findRefreshTokenNullKey() {
		// given
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(REFRESH_TOKEN_KEY)).thenReturn(null);

		// when
		String resultRefreshToken = redisTokenService.getRefreshToken(EMAIL);

		// then
		assertThat(resultRefreshToken).isNull();
		verify(valueOperations, times(1)).get(REFRESH_TOKEN_KEY);
	}

	@Test
	@DisplayName("RefreshToken을 Redis에서 삭제한다")
	void deleteRefreshToken() {
		// given
		when(redisTemplate.delete(REFRESH_TOKEN_KEY)).thenReturn(true);

		// when
		Boolean result = redisTokenService.deleteRefreshToken(EMAIL);

		// then
		assertThat(result).isTrue();
		verify(redisTemplate, times(1)).delete(REFRESH_TOKEN_KEY);
	}

	@Test
	@DisplayName("Redis에서_RefreshToken_삭제에_실패한다")
	void deleteRefreshTokenFailed() {
		// given
		when(redisTemplate.delete(REFRESH_TOKEN_KEY)).thenReturn(false);

		// when
		Boolean result = redisTokenService.deleteRefreshToken(EMAIL);

		// then
		assertThat(result).isFalse();
		verify(redisTemplate, times(1)).delete(REFRESH_TOKEN_KEY);
	}

	@Test
	@DisplayName("OAuth2 AccessToken을 Redis에 저장한다")
	void saveOAuth2AccessToken() {
		// given
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);

		// when
		redisTokenService.saveOauth2AccessToken(PROVIDER, OAUTH2_ACCESS_TOKEN, EXPIRATION_SECONDS);

		// then
		verify(valueOperations, times(1))
			.set(OAUTH2_ACCESS_TOKEN_KEY, OAUTH2_ACCESS_TOKEN, EXPIRATION_SECONDS, TimeUnit.SECONDS);
	}

	@Test
	@DisplayName("Key에 해당하는 OAuth2AccessToken을 Redis에서_조회한다")
	void findOAuth2AccessToken() {
		// given
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(OAUTH2_ACCESS_TOKEN_KEY)).thenReturn(OAUTH2_ACCESS_TOKEN);

		// when
		String resultOAuth2AccessToken = redisTokenService.getOauth2AccessToken(PROVIDER);

		// then
		assertThat(resultOAuth2AccessToken).isEqualTo(OAUTH2_ACCESS_TOKEN);
		verify(valueOperations, times(1)).get(OAUTH2_ACCESS_TOKEN_KEY);
	}

	@Test
	@DisplayName("존재하지 않는 Key로 OAuth2 AccessToken을 Redis에서 조회시 null을 반환한다")
	void findOAuth2AccessTokenNullKey() {
		// given
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(OAUTH2_ACCESS_TOKEN_KEY)).thenReturn(null);

		// when
		String resultRefreshToken = redisTokenService.getOauth2AccessToken(PROVIDER);

		// then
		assertThat(resultRefreshToken).isNull();
		verify(valueOperations, times(1)).get(OAUTH2_ACCESS_TOKEN_KEY);
	}

	@Test
	@DisplayName("OAuth2 AccessToken을 Redis에서 삭제한다")
	void deleteOAuth2AccessToken() {
		// given
		when(redisTemplate.delete(OAUTH2_ACCESS_TOKEN_KEY)).thenReturn(true);

		// when
		Boolean result = redisTokenService.deleteOauth2AccessToken(PROVIDER);

		// then
		assertThat(result).isTrue();
		verify(redisTemplate, times(1)).delete(OAUTH2_ACCESS_TOKEN_KEY);
	}

	@Test
	@DisplayName("Redis에서 OAuth2 AccessToken_삭제에 실패한다")
	void deleteOAuth2AccessTokenFailed() {
		// given
		when(redisTemplate.delete(OAUTH2_ACCESS_TOKEN_KEY)).thenReturn(false);

		// when
		Boolean result = redisTokenService.deleteOauth2AccessToken(PROVIDER);

		// then
		assertThat(result).isFalse();
		verify(redisTemplate, times(1)).delete(OAUTH2_ACCESS_TOKEN_KEY);
	}
}
