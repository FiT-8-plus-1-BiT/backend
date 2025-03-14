package eightplusone.bit.fit.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class UserAccountResponseDto {
	private final String name;
	private final String email;

	@Builder
	private UserAccountResponseDto(String name, String email) {
		this.name = name;
		this.email = email;
	}

	public static UserAccountResponseDto of(String name, String email) {
		return UserAccountResponseDto.builder()
			.name(name)
			.email(email)
			.build();
	}
}
