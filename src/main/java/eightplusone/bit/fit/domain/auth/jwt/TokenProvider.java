package eightplusone.bit.fit.domain.auth.jwt;

import static eightplusone.bit.fit.global.constants.TokenConstant.*;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import eightplusone.bit.fit.domain.auth.dto.CustomUserDetails;
import eightplusone.bit.fit.domain.auth.service.CustomUserDetailsService;
import eightplusone.bit.fit.domain.auth.service.RedisTokenService;
import eightplusone.bit.fit.global.exception.CustomException;
import eightplusone.bit.fit.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TokenProvider {

	private final RedisTokenService redisTokenService;
	private final CustomUserDetailsService customUserDetailsService;
	private final SecretKey accessTokenSigningKey;
	private final SecretKey refreshTokenSigningKey;
	private final long accessTokenExpirationSeconds;
	private final long refreshTokenExpirationSeconds;

	public TokenProvider(
		RedisTokenService redisTokenService,
		CustomUserDetailsService customUserDetailsService,
		@Value("${spring.jwt.access-secret-key}") String accessTokenSecret,
		@Value("${spring.jwt.refresh-secret-key}") String refreshTokenSecret,
		@Value("${spring.jwt.access-token-validity-in-seconds}") Long accessTokenExpirationSeconds,
		@Value("${spring.jwt.refresh-token-validity-in-seconds}") Long refreshTokenExpirationSeconds) {
		this.redisTokenService = redisTokenService;
		this.customUserDetailsService = customUserDetailsService;
		this.accessTokenSigningKey = Keys.hmacShaKeyFor(accessTokenSecret.getBytes(StandardCharsets.UTF_8));
		this.refreshTokenSigningKey = Keys.hmacShaKeyFor(refreshTokenSecret.getBytes(StandardCharsets.UTF_8));
		this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
		this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
	}

	public String createAccessToken(String email, String roles, Date now) {
		long accessTokenExpirationMilliseconds = accessTokenExpirationSeconds * 1000;
		Date accessExpiredTime = new Date(now.getTime() + accessTokenExpirationMilliseconds);

		return Jwts.builder()
			.subject(email)
			.claim(AUTHORITIES_KEY, roles)
			.issuedAt(now)
			.expiration(accessExpiredTime)
			.signWith(accessTokenSigningKey)
			.compact();
	}

	public String createRefreshToken(String email, Date now) {
		long refreshTokenExpirationMilliseconds = refreshTokenExpirationSeconds * 1000;
		Date refreshExpiredTime = new Date(now.getTime() + refreshTokenExpirationMilliseconds);

		String refreshToken = Jwts.builder()
			.subject(email)
			.issuedAt(now)
			.expiration(refreshExpiredTime)
			.signWith(refreshTokenSigningKey)
			.compact();
		redisTokenService.saveRefreshToken(email, refreshToken, refreshTokenExpirationSeconds);

		return refreshToken;
	}

	public String resolveAccessToken(HttpServletRequest request) {
		String requestAccessTokenInHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (requestAccessTokenInHeader != null && requestAccessTokenInHeader.startsWith(BEARER_PREFIX)) {
			return requestAccessTokenInHeader.substring(BEARER_PREFIX.length());
		}
		return null;
	}

	public Authentication getAuthenticationByAccessToken(String accessToken) {
		Claims claims = getClaimsByAccessToken(accessToken);
		String subject = claims.getSubject();

		CustomUserDetails customUserDetails = customUserDetailsService.loadUserByUsername(subject);
		return new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
	}

	public boolean validateAccessToken(String token) {
		return validateToken(accessTokenSigningKey, token);
	}

	public boolean validateRefreshToken(String token) {
		return validateToken(refreshTokenSigningKey, token);
	}

	public Claims getClaimsByAccessToken(String accessToken) {
		return getClaims(accessTokenSigningKey, accessToken);
	}

	public Claims getClaimsRefreshToken(String refreshToken) {
		return getClaims(refreshTokenSigningKey, refreshToken);
	}

	private Claims getClaims(SecretKey secretKey, String token) {
		try {
			return Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
		} catch (ExpiredJwtException e) {
			return e.getClaims();
		} catch (JwtException e) {
			log.error("잘못된 jwt 토큰입니다. {}", e.getMessage());
			throw new CustomException(ErrorCode.INVALID_AUTH_TOKEN);
		} catch (Exception e) {
			throw new CustomException(ErrorCode.INVALID_AUTH_TOKEN);
		}
	}

	private boolean validateToken(SecretKey key, String token) {
		if (token == null) {
			log.error("jwt 토큰이 비어 있습니다.");
			return false;
		}
		try {
			Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
			return true;
		} catch (SignatureException e) {
			log.error("잘못된 jwt 서명입니다.");
		} catch (MalformedJwtException e) {
			log.error("잘못된 jwt 토큰입니다.");
		} catch (ExpiredJwtException e) {
			log.error("만료된 jwt 토큰입니다.");
		} catch (UnsupportedJwtException e) {
			log.error("지원되지 않는 jwt 토큰입니다.");
		} catch (IllegalArgumentException e) {
			log.error("jwt 클레임 문자열이 비어 있습니다.");
		}
		return false;
	}

	public void invalidateRefreshToken(String subject) {
		redisTokenService.deleteRefreshToken(subject);
	}

	public boolean validateRefreshTokenWithAccessTokenInfo(String role, String subject, String requestRefreshToken) {
		String refreshToken = redisTokenService.getRefreshToken(subject);

		// 무결성 검증이 실패한 경우 초기화 진행
		if (!requestRefreshToken.equals(refreshToken)) {
			log.warn("{}-{}: reissue fail ({})", subject, role, new Date());
			redisTokenService.deleteRefreshToken(subject);
			return false;
		}
		log.info("{}-{}: reissue ({})", subject, role, new Date());
		return true;
	}

	public long getAccessTokenExpirationSeconds() {
		return accessTokenExpirationSeconds;
	}

	public long getRefreshTokenExpirationSeconds() {
		return refreshTokenExpirationSeconds;
	}
}
