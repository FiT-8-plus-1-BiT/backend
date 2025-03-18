package eightplusone.bit.fit.domain.auth.service;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisTokenService {

	private final RedisTemplate<String, String> redisTemplate;

	private static final String REFRESH_PREFIX = "RT:";
	private static final String OAUTH2_PREFIX = "AT:";

	public void saveRefreshToken(String email, String refreshToken, long refreshTokenExpirationSeconds) {
		redisTemplate.opsForValue()
			.set(REFRESH_PREFIX + email, refreshToken, refreshTokenExpirationSeconds, TimeUnit.SECONDS);
	}

	public String getRefreshToken(String email) {
		return redisTemplate.opsForValue().get(REFRESH_PREFIX + email);
	}

	public Boolean deleteRefreshToken(String email) {
		return redisTemplate.delete(REFRESH_PREFIX + email);
	}

	public void saveOauth2AccessToken(String provider, String oauth2AccessToken, long oauth2ValidityInMilliseconds) {
		redisTemplate.opsForValue()
			.set(OAUTH2_PREFIX + provider, oauth2AccessToken, oauth2ValidityInMilliseconds, TimeUnit.SECONDS);
	}

	public String getOauth2AccessToken(String provider) {
		return redisTemplate.opsForValue().get(OAUTH2_PREFIX + provider);
	}

	public Boolean deleteOauth2AccessToken(String provider) {
		return redisTemplate.delete(OAUTH2_PREFIX + provider);
	}
}
