package eightplusone.bit.fit.global.utils;

import org.springframework.http.ResponseCookie;

import eightplusone.bit.fit.global.exception.CustomException;
import eightplusone.bit.fit.global.exception.ErrorCode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

public final class CookieUtil {

	public static ResponseCookie createCookie(String name, String value, long cookieExpiration) {
		return ResponseCookie.from(name, value)
			.maxAge(cookieExpiration)
			.path("/")
			.sameSite("Strict")
			.httpOnly(true)
			.build();
	}

	public static Cookie findCookieByName(HttpServletRequest request, String name) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals(name)) {
					return cookie;
				}
			}
		}
		throw new CustomException(ErrorCode.INVALID_REQUEST);
	}
}
