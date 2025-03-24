package eightplusone.bit.fit.domain.session.dto;

import eightplusone.bit.fit.domain.session.entity.Session;
import eightplusone.bit.fit.domain.speaker.dto.SpeakerResponseDto;
import eightplusone.bit.fit.domain.tag.dto.TagDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(name = "SessionListResponseDto: 전체 세션 조회 Dto")
public class SessionListResponseDto {
	@Schema(description = "", example = "")
	private Long id;

	@Schema(description = "", example = "")
	private String title;

	@Schema(description = "", example = "")
	private String summary;

	private Boolean isMySession;

	private SpeakerResponseDto speaker;

	private TagDto tags;

	public static SessionListResponseDto from(Session session, SpeakerResponseDto speaker, TagDto tags,
		Boolean isMySession) {
		return SessionListResponseDto.builder()
			.id(session.getSessionId())
			.title(session.getTitle())
			.summary(session.getSummary())
			.isMySession(isMySession)
			.speaker(speaker)
			.tags(tags)
			.build();
	}
}
