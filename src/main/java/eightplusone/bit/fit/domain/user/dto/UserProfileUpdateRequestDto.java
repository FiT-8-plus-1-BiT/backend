package eightplusone.bit.fit.domain.user.dto;

import java.util.List;

import eightplusone.bit.fit.domain.user.entity.enums.YearLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(name = "UserProfileUpdateRequestDto: 회원 개인 정보 업데이트 Dto")
public class UserProfileUpdateRequestDto {

	@NotBlank(message = "이름이 입력되지 않았습니다.")
	@Schema(description = "사용자 이름", example = "홍길동")
	private String name;

	@NotBlank(message = "직무가 입력되지 않았습니다.")
	@Schema(description = "직무", example = "백엔드")
	private String job;

	@NotNull(message = "연차가 입력되지 않았습니다.")
	@Schema(description = "연차", example = "신입")
	private YearLevel years;

	@NotEmpty(message = "관심 분야가 되지않았습니다.")
	@Size(min = 3, max = 3, message = "관심 분야는 3개 선택해야 합니다.")
	@Schema(description = "사용자의 관심 분야 목록", example = "[\"클라우드\", \"데이터베이스\", \"자바스크립트\"]")
	private List<String> interests;

	@Builder
	private UserProfileUpdateRequestDto(String name, String job, YearLevel years, List<String> interests) {
		this.name = name;
		this.job = job;
		this.years = years;
		this.interests = interests;
	}

	public static UserProfileUpdateRequestDto of(String name, String job, YearLevel years, List<String> interests) {
		return UserProfileUpdateRequestDto.builder()
			.name(name)
			.job(job)
			.years(years)
			.interests(interests)
			.build();
	}
}
