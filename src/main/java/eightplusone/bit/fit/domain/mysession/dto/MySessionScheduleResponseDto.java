package eightplusone.bit.fit.domain.mysession.dto;

import java.time.format.DateTimeFormatter;

import eightplusone.bit.fit.domain.speaker.entity.Speaker;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Schema(name = "MySessionScheduleResponseDto: 미리 담기된 강연 스케쥴 Dto")
public class MySessionScheduleResponseDto {
	@Schema(description = "강연 식별자", example = "1")
	private final Long sessionId;
	@Schema(description = "강연 제목", example = "프론트엔드 마스터하기")
	private final String title;
	@Schema(description = "강연 이미지", example = "image.png")
	private final String sessionImage;
	@Schema(description = "강연 요약", example = "JS를 잘하는법")
	private final String summary;
	@Schema(description = "강연 시작 시간", example = "202504020950")
	private final String startTime;
	@Schema(description = "강연 종료 시간", example = "202504021050")
	private final String endTime;
	@Schema(description = "최대 수용 인원", example = "70")
	private final Integer standardCount;
	@Schema(description = "오디오 채널방 번호", example = "1")
	private final Integer audioChannel;
	@Schema(description = "담은 스케줄인지 여부", example = "true")
	private final Boolean isMySchedule;
	@Schema(description = "강연자 이름", example = "홍길동")
	private final String speakerName;
	@Schema(description = "강연자 이미지", example = "http://localhost:8080/speaker.png")
	private final String speakerImage;

	@Builder
	private MySessionScheduleResponseDto(Long sessionId, String title, String sessionImage, String summary,
		String startTime, String endTime, Integer standardCount, Integer audioChannel, Boolean isMySchedule,
		String speakerName, String speakerImage) {
		this.sessionId = sessionId;
		this.title = title;
		this.sessionImage = sessionImage;
		this.summary = summary;
		this.startTime = startTime;
		this.endTime = endTime;
		this.standardCount = standardCount;
		this.audioChannel = audioChannel;
		this.isMySchedule = isMySchedule;
		this.speakerName = speakerName;
		this.speakerImage = speakerImage;
	}

	public static MySessionScheduleResponseDto from(Speaker speaker, Boolean isMySchedule) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMddHHmm");
		return MySessionScheduleResponseDto.builder()
			.sessionId(speaker.getSession().getSessionId())
			.title(speaker.getSession().getTitle())
			.sessionImage(speaker.getSession().getSessionImage())
			.summary(speaker.getSession().getSummary())
			.startTime(speaker.getSession().getStartTime().format(formatter))
			.endTime(speaker.getSession().getEndTime().format(formatter))
			.standardCount(speaker.getSession().getStandardCount())
			.audioChannel(speaker.getSession().getAudioChannel())
			.isMySchedule(isMySchedule)
			.speakerName(speaker.getName())
			.speakerImage(speaker.getImage())
			.build();
	}
}
