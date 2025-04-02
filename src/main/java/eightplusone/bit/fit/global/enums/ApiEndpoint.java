package eightplusone.bit.fit.global.enums;

import org.springframework.http.HttpMethod;

import lombok.Getter;

@Getter
public enum ApiEndpoint {

	PUBLIC_GET(HttpMethod.GET, new String[] {
		"/swagger-ui/**",
		"/actuator/**",
		"/v3/**",
		"/call",
		"/ws",
		"/ws/**",
		"/api/v1/session/**",
		"/api/v1/speaker",
		"/health"
	}),

	PUBLIC_POST(HttpMethod.POST, new String[] {
		"/api/v1/auth/reissue",
		"/api/v1/auth/token-exchange",
	}),

	AUTHENTICATED_GET(HttpMethod.GET, new String[] {
		"/api/v1/users/account",
		"/api/v1/users/profile",
		"/api/v1/users/profile/image",
		"/api/v1/users/sessions",
		"/api/v1/users/sessions/like"
	}),

	AUTHENTICATED_POST(HttpMethod.POST, new String[] {
		"/api/v1/auth/logout",
		"/api/v1/enter/checkin",
		"/api/v1/enter/checkout",
		"/api/v1/users/sessions",
		"/api/v1/users/sessions/like"
	}),

	AUTHENTICATED_PUT(HttpMethod.PUT, new String[] {
		"/api/v1/users/profile",
		"/api/v1/users/profile/image"
	}),

	AUTHENTICATED_PATCH(HttpMethod.PATCH, new String[] {
	}),

	AUTHENTICATED_DELETE(HttpMethod.DELETE, new String[] {
		"/api/v1/users",
		"/api/v1/users/profile/image",
		"/api/v1/users/sessions",
		"/api/v1/users/sessions/like"
	});

	private final HttpMethod method;
	private final String[] paths;

	ApiEndpoint(HttpMethod method, String[] paths) {
		this.method = method;
		this.paths = paths;
	}
}

