package eightplusone.bit.fit.global.config;

import static eightplusone.bit.fit.global.constants.CorsConstant.*;
import static eightplusone.bit.fit.global.enums.ApiEndpoint.*;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;

import eightplusone.bit.fit.domain.auth.enums.Role;
import eightplusone.bit.fit.domain.auth.filter.CustomLogoutFilter;
import eightplusone.bit.fit.domain.auth.filter.JwtAuthenticationFilter;
import eightplusone.bit.fit.domain.auth.handler.CustomAccessDeniedHandler;
import eightplusone.bit.fit.domain.auth.handler.CustomAuthenticationEntryPoint;
import eightplusone.bit.fit.domain.auth.handler.CustomOAuth2AuthenticationFailureHandler;
import eightplusone.bit.fit.domain.auth.handler.CustomOAuth2SuccessHandler;
import eightplusone.bit.fit.domain.auth.jwt.TokenProvider;
import eightplusone.bit.fit.domain.auth.service.CustomOAuth2UserService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final String allowedOrigins;
	private final TokenProvider tokenProvider;
	private final CustomOAuth2UserService customOAuth2UserService;
	private final ObjectMapper objectMapper;

	public SecurityConfig(
		@Value("${cors.allow.origins}") String allowedOrigins,
		TokenProvider tokenProvider,
		CustomOAuth2UserService customOAuth2UserService,
		ObjectMapper objectMapper) {
		this.allowedOrigins = allowedOrigins;
		this.tokenProvider = tokenProvider;
		this.customOAuth2UserService = customOAuth2UserService;
		this.objectMapper = objectMapper;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.cors(corsCustomizer -> corsCustomizer.configurationSource(request -> {
				CorsConfiguration configuration = new CorsConfiguration();
				configuration.setAllowedOrigins(
					List.of("https://jiangxy.github.io", "http://localhost:5173")); // TODO : 혼잡도 구현 끝날 경우 원본으로 되돌리기
				configuration.setAllowedMethods(ALLOWED_METHODS);
				configuration.setAllowCredentials(ALLOWED_CREDENTIALS);
				configuration.setAllowedHeaders(ALLOWED_HEADERS);
				configuration.setMaxAge(MAX_AGE);
				configuration.setExposedHeaders(Collections.singletonList(HttpHeaders.SET_COOKIE));
				configuration.setExposedHeaders(Collections.singletonList(HttpHeaders.AUTHORIZATION));
				return configuration;
			}))
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(PUBLIC_GET.getMethod(), PUBLIC_GET.getPaths())
				.permitAll()
				.requestMatchers(PUBLIC_POST.getMethod(), PUBLIC_POST.getPaths())
				.permitAll()

				.requestMatchers(AUTHENTICATED_GET.getMethod(), AUTHENTICATED_GET.getPaths())
				.hasAuthority(Role.USER.getKey())
				.requestMatchers(AUTHENTICATED_POST.getMethod(), AUTHENTICATED_POST.getPaths())
				.hasAuthority(Role.USER.getKey())
				.requestMatchers(AUTHENTICATED_PUT.getMethod(), AUTHENTICATED_PUT.getPaths())
				.hasAuthority(Role.USER.getKey())
				.requestMatchers(AUTHENTICATED_PATCH.getMethod(), AUTHENTICATED_PATCH.getPaths())
				.hasAuthority(Role.USER.getKey())
				.requestMatchers(AUTHENTICATED_DELETE.getMethod(), AUTHENTICATED_DELETE.getPaths())
				.hasAuthority(Role.USER.getKey())

				.anyRequest()
				.permitAll() // 모든 요청 허용
			)
			.addFilterAfter(new JwtAuthenticationFilter(tokenProvider), UsernamePasswordAuthenticationFilter.class)
			.addFilterBefore(new CustomLogoutFilter(tokenProvider, objectMapper), LogoutFilter.class)
			.oauth2Login((oauth2) -> oauth2.userInfoEndpoint(
					(userInfoEndpointConfig -> userInfoEndpointConfig.userService(customOAuth2UserService)))
				.successHandler(new CustomOAuth2SuccessHandler(tokenProvider, allowedOrigins))
				.failureHandler(new CustomOAuth2AuthenticationFailureHandler(allowedOrigins)))
			.exceptionHandling((exceptionHandling) -> exceptionHandling
				.authenticationEntryPoint(new CustomAuthenticationEntryPoint(objectMapper))
				.accessDeniedHandler(new CustomAccessDeniedHandler(objectMapper)));

		return http.build();
	}
}
