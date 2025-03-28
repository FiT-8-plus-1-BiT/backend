package eightplusone.bit.fit.domain.auth.handler;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomOAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

	private final String allowedOrigins;

	public CustomOAuth2AuthenticationFailureHandler(@Value("${cors.allow.origins}") String allowedOrigins) {
		this.allowedOrigins = allowedOrigins;
	}

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
		AuthenticationException exception) throws IOException {
		String errorMessage = URLEncoder.encode("social_login_duplicate", StandardCharsets.UTF_8);
		if (request.getRequestURI().contains("naver")) {
			response.sendRedirect(allowedOrigins + "/signup" + "?error=" + errorMessage);
		} else {
			response.sendRedirect("http://localhost:5173/signup?error=" + errorMessage);
		}
	}
}
