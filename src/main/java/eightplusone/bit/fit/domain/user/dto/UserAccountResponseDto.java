package eightplusone.bit.fit.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Schema(name = "UserAccountResponseDto: 회원 계정 정보 Dto")
public class UserAccountResponseDto {

	@Schema(description = "사용자 이름", example = "홍길동")
	private final String name;
	@Schema(description = "사용자 이메일", example = "test@gmail.com")
	private final String email;
	@Schema(description = "사용자 프로필 이미지", example = "https://12341234.s3.ap-northeast-2.amazonaws.com/12341234_profile.jpg ")
	private final String imageUrl;

	@Builder
	private UserAccountResponseDto(String name, String email, String imageUrl) {
		this.name = name;
		this.email = email;
		this.imageUrl = imageUrl;
	}

	public static UserAccountResponseDto of(String name, String email, String imageUrl) {
		return UserAccountResponseDto.builder()
			.name(name)
			.email(email)
			.imageUrl(imageUrl)
			.build();
	}
}
