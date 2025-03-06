package eightplusone.bit.fit.global.constants;

import lombok.Getter;

@Getter
public class TokenConstant {
	public static final String AUTHORITIES_KEY = "role";
	public static final String EMAIL_KEY = "email";

	public static final String BEARER_PREFIX = "Bearer ";
	public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
	public static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
	public static final int REFRESH_EXPIRATION_DELETE = 0;
}
