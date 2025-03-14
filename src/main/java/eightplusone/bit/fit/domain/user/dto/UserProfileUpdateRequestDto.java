package eightplusone.bit.fit.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(name = "UserProfileUpdateRequestDto: 회원 개인 정보 업데이트 Dto")
public class UserProfileUpdateRequestDto {

	@NotBlank(message = "직무가 입력되지 않았습니다.")
	@Schema(description = "직무", example = "백엔드")
	private String job;

	@Min(value = 0, message = "연차는 0 이상이어야 합니다.")
	@NotNull(message = "연차가 입력되지 않았습니다.")
	@Schema(description = "연차", example = "1")
	private Integer years;

	@NotBlank(message = "관심 분야가 입력되지 않았습니다.")
	@Schema(description = "관심 분야", example = "클라우드")
	private String interests;

	@Builder
	private UserProfileUpdateRequestDto(String job, Integer years, String interests) {
		this.job = job;
		this.years = years;
		this.interests = interests;
	}

	public static UserProfileUpdateRequestDto of(String job, Integer years, String interests) {
		return UserProfileUpdateRequestDto.builder()
			.job(job)
			.years(years)
			.interests(interests)
			.build();
	}
}
