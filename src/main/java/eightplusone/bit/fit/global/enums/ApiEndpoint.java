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
		"/ws-chat",
		"/ws-chat/**",
		"/ws-room"
	}),

	PUBLIC_POST(HttpMethod.POST, new String[] {
		"/api/v1/auth/reissue",
		"/api/v1/auth/token-exchange",
	}),

	AUTHENTICATED_GET(HttpMethod.GET, new String[] {
		"/api/v1/users/account",
		"/api/v1/users/profile"
	}),

	AUTHENTICATED_POST(HttpMethod.POST, new String[] {
		"/api/v1/auth/logout",
		"/api/v1/enter/checkin",
		"/api/v1/enter/checkout"
	}),

	AUTHENTICATED_PUT(HttpMethod.PUT, new String[] {
		"/api/v1/users/profile"
	}),

	AUTHENTICATED_PATCH(HttpMethod.PATCH, new String[] {
	}),

	AUTHENTICATED_DELETE(HttpMethod.DELETE, new String[] {
		"/api/v1/users"
	});

	private final HttpMethod method;
	private final String[] paths;

	ApiEndpoint(HttpMethod method, String[] paths) {
		this.method = method;
		this.paths = paths;
	}
}

