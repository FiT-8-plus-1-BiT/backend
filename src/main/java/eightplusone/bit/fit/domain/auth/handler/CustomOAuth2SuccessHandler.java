package eightplusone.bit.fit.domain.auth.handler;

import static eightplusone.bit.fit.global.constants.TokenConstant.*;

import java.io.IOException;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import eightplusone.bit.fit.domain.auth.dto.CustomOAuth2User;
import eightplusone.bit.fit.domain.auth.jwt.TokenProvider;
import eightplusone.bit.fit.global.utils.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CustomOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final TokenProvider tokenProvider;
	private final String allowedOrigins;

	public CustomOAuth2SuccessHandler(TokenProvider tokenProvider,
		@Value("${cors.allow.origins}") String allowedOrigins) {
		this.tokenProvider = tokenProvider;
		this.allowedOrigins = allowedOrigins;
	}

	@Override
	public void onAuthenticationSuccess(
		HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

		//OAuth2User
		CustomOAuth2User customUserDetails = (CustomOAuth2User)authentication.getPrincipal();

		String email = customUserDetails.getName();
		String role = authentication.getAuthorities().iterator().next().getAuthority();

		String accessToken = tokenProvider.createAccessToken(email, role, new Date());
		String refreshToken = tokenProvider.createRefreshToken(email, new Date());

		response.addHeader(HttpHeaders.SET_COOKIE,
			CookieUtil.createCookie(ACCESS_TOKEN_COOKIE_NAME, accessToken,
				tokenProvider.getAccessTokenExpirationSeconds()).toString());
		response.addHeader(HttpHeaders.SET_COOKIE,
			CookieUtil.createCookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken,
				tokenProvider.getRefreshTokenExpirationSeconds()).toString());
		if (request.getRequestURI().contains("naver")) {
			response.sendRedirect(allowedOrigins + "/main");
		} else {
			response.sendRedirect("http://localhost:5173/main");
		}
		log.info("{}-{}: login ({})", email, role, new Date());
	}
}
