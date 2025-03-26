package eightplusone.bit.fit.domain.user.dto;

import java.util.List;

import eightplusone.bit.fit.domain.interest.entity.MyInterest;
import eightplusone.bit.fit.domain.user.entity.enums.YearLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Schema(name = "UserProfileResponseDto: 회원 개인 정보 Dto")
public class UserProfileResponseDto {

	@Schema(description = "직무", example = "백엔드")
	private final String job;
	@Schema(description = "연차", example = "1")
	private final YearLevel years;
	@Schema(description = "사용자의 관심 분야", example = "[\"클라우드\", \"데이터베이스\", \"자바스크립트\"]")
	private final List<String> interests;

	@Builder
	private UserProfileResponseDto(String job, YearLevel years, List<String> interests) {
		this.job = job;
		this.years = years;
		this.interests = interests;
	}

	public static UserProfileResponseDto of(String job, YearLevel years, List<MyInterest> myInterests) {
		return UserProfileResponseDto.builder()
			.job(job)
			.years(years)
			.interests(myInterests.stream().map(myInterest -> myInterest.getInterest().getName()).toList())
			.build();
	}
}
