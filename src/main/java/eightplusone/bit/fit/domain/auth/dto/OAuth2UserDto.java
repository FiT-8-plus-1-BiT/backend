package eightplusone.bit.fit.domain.auth.dto;

import eightplusone.bit.fit.domain.auth.enums.Role;
import lombok.Builder;
import lombok.Getter;

@Getter
public class OAuth2UserDto {

	private final String provider;
	private final String email;
	private final String name;
	private final Role role;

	@Builder
	private OAuth2UserDto(String provider, String email, String name, Role role) {
		this.provider = provider;
		this.email = email;
		this.name = name;
		this.role = role;
	}

	public static OAuth2UserDto of(String provider, String email, String name, Role role) {
		return OAuth2UserDto.builder()
			.provider(provider)
			.email(email)
			.name(name)
			.role(role)
			.build();
	}
}
