package eightplusone.bit.fit.domain.tag.dto;

import eightplusone.bit.fit.domain.tag.entity.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(name = "TagDto: 태그 정보 응답 및 필터링에 사용되 dto")
public class TagDto {
	private String field;

	private String topic;

	private String type;

	private String level;

	public static TagDto from(Tag tag) {
		return TagDto.builder()
			.field(tag.getField())
			.topic(tag.getTopic())
			.type(tag.getType())
			.level(tag.getLevel())
			.build();
	}
}
