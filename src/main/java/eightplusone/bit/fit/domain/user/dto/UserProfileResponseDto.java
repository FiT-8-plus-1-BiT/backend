package eightplusone.bit.fit.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Schema(name = "UserProfileResponseDto: 회원 개인 정보 Dto")
public class UserProfileResponseDto {

	@Schema(description = "직무", example = "백엔드")
	private final String job;
	@Schema(description = "연차", example = "1")
	private final Integer years;
	@Schema(description = "관심 분야", example = "클라우드")
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
