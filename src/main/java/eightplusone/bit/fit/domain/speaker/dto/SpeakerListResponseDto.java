package eightplusone.bit.fit.domain.speaker.dto;

import java.time.format.DateTimeFormatter;

import eightplusone.bit.fit.domain.session.entity.Session;
import eightplusone.bit.fit.domain.tag.dto.TagDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(name = "SpeakerListResponseDto: 메인페이지 연사 정보 dto")
public class SpeakerListResponseDto {
	@Schema(description = "세션 제목", example = "디지털 송금 혁명의 미래")
	private String title;

	@Schema(description = "강연 시작 시간", example = "202504020950")
	private final String startTime;

	@Schema(description = "강연 종료 시간", example = "202504021050")
	private final String endTime;

	@Schema(description = "태그 정보")
	private TagDto tags;

	@Schema(description = "연사 정보")
	private SpeakerResponseDto speaker;

	public static SpeakerListResponseDto from(Session session, SpeakerResponseDto speaker, TagDto tags) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMddHHmm");

		return SpeakerListResponseDto.builder().title(session.getTitle())
			.startTime(session.getStartTime().format(formatter))
			.endTime(session.getEndTime().format(formatter))
			.speaker(speaker).tags(tags).build();
	}
}
