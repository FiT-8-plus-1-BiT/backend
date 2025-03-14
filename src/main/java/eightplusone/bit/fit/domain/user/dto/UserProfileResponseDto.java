package eightplusone.bit.fit.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class UserProfileResponseDto {
	private final String job;
	private final Integer years;
	private final String interests;

	@Builder
	private UserProfileResponseDto(String job, Integer years, String interests) {
		this.job = job;
		this.years = years;
		this.interests = interests;
	}

	public static UserProfileResponseDto of(String job, Integer years, String interests) {
		return UserProfileResponseDto.builder()
			.job(job)
			.years(years)
			.interests(interests)
			.build();
	}
}
