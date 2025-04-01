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
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
		log.info("소셜 로그인 URL 체크: " + request.getRequestURI());
		log.info("Origins 체크: " + allowedOrigins);
		if (request.getRequestURI().contains("google")) {
			response.sendRedirect("http://localhost:5173/signup?error=" + errorMessage);
			log.info("//==로컬 이용==//");
		} else {
			response.sendRedirect(allowedOrigins + "/signup" + "?error=" + errorMessage);
			log.info("//==Origin 이용==//");
		}
	}
}
