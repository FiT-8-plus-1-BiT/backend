package eightplusone.bit.fit.domain.speaker.dto;

import eightplusone.bit.fit.domain.speaker.entity.Speaker;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(name = "SpeakerResponseDto: 연사 정보 응답 dto")
public class SpeakerResponseDto {
	private String image;

	private String name;

	private String description;

	public static SpeakerResponseDto from(Speaker speaker) {
		return SpeakerResponseDto.builder()
			.image(speaker.getImage())
			.name(speaker.getName())
			.description(speaker.getDescription())
			.build();
	}
}
