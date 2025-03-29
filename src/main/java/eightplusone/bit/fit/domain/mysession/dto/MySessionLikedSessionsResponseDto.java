package eightplusone.bit.fit.domain.mysession.dto;

import java.time.format.DateTimeFormatter;

import eightplusone.bit.fit.domain.speaker.entity.Speaker;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Schema(name = "MySessionLikedSessionsResponseDto: 좋아요 담기한 나의 세션 리스트 Dto")
public class MySessionLikedSessionsResponseDto {
	@Schema(description = "강연 식별자", example = "1")
	private final Long sessionId;
	@Schema(description = "강연 제목", example = "백엔드 마스터하기")
	private final String title;
	@Schema(description = "강연 요약", example = "자바 잘하는법")
	private final String summary;
	@Schema(description = "강연 시작 시간", example = "202504020950")
	private final String startTime;
	@Schema(description = "강연 종료 시간", example = "202504021050")
	private final String endTime;
	@Schema(description = "강연자 이름", example = "홍길동")
	private final String speakerName;
	@Schema(description = "강연자 이미지", example = "http://localhost:8080/speaker.png")
	private final String speakerImage;

	@Builder
	private MySessionLikedSessionsResponseDto(Long sessionId, String title, String summary, String startTime,
		String endTime, String speakerName, String speakerImage) {
		this.sessionId = sessionId;
		this.title = title;
		this.summary = summary;
		this.startTime = startTime;
		this.endTime = endTime;
		this.speakerName = speakerName;
		this.speakerImage = speakerImage;
	}

	public static MySessionLikedSessionsResponseDto from(Speaker speaker) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMddHHmm");
		return MySessionLikedSessionsResponseDto.builder()
			.sessionId(speaker.getSession().getSessionId())
			.title(speaker.getSession().getTitle())
			.summary(speaker.getSession().getSummary())
			.startTime(speaker.getSession().getStartTime().format(formatter))
			.endTime(speaker.getSession().getEndTime().format(formatter))
			.speakerName(speaker.getName())
			.speakerImage(speaker.getImage())
			.build();
	}
}
