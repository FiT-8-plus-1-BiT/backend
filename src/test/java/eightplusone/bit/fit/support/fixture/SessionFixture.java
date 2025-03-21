package eightplusone.bit.fit.support.fixture;

import java.time.LocalDateTime;

import eightplusone.bit.fit.domain.session.entity.Session;
import lombok.Getter;

@Getter
public enum SessionFixture {
	/**
	 * 1부 세션 1,2,3
	 */
	SESSION_STAGE_1_FIXTURE_1("1부 세션1 09:50-10:50", "백엔드 잘하는 법", "image1_1.png",
		LocalDateTime.now(), LocalDateTime.now().plusHours(3), 100, 2),
	SESSION_STAGE_1_FIXTURE_2("1부 세션2 09:50-10:50", "프론트엔드 마스터하기", "image1_2.png",
		LocalDateTime.now(), LocalDateTime.now().plusHours(3), 50, 1),
	SESSION_STAGE_1_FIXTURE_3("1부 세션3 09:50-10:50", "데이터베이스 성능 최적화", "image1_3.png",
		LocalDateTime.of(2024, 4, 2, 9, 50), LocalDateTime.of(2024, 4, 2, 10, 50), 70, 3),

	/**
	 * 2부 세션 1,2,3
	 */
	SESSION_STAGE_2_FIXTURE_1("2부 세션1 11:50-12:50", "백엔드 잘하는 법", "image2_1.png",
		LocalDateTime.of(2024, 4, 2, 11, 50), LocalDateTime.of(2024, 4, 2, 12, 50), 100, 2),
	SESSION_STAGE_2_FIXTURE_2("2부 세션2 11:50-12:50", "프론트엔드 마스터하기", "image2_2.png",
		LocalDateTime.of(2024, 4, 2, 11, 50), LocalDateTime.of(2024, 4, 2, 12, 50), 50, 1),
	SESSION_STAGE_2_FIXTURE_3("2부 세션3 11:50-12:50", "데이터베이스 성능 최적화", "image2_3.png",
		LocalDateTime.of(2024, 4, 2, 11, 50), LocalDateTime.of(2024, 4, 2, 12, 50), 70, 3),

	/**
	 * 3부 세션 1,2,3
	 */
	SESSION_STAGE_3_FIXTURE_1("3부 세션1 01:50-02:50", "백엔드 잘하는 법", "image3_1.png",
		LocalDateTime.of(2024, 4, 2, 13, 50), LocalDateTime.of(2024, 4, 2, 14, 50), 100, 2),
	SESSION_STAGE_3_FIXTURE_2("3부 세션2 01:50-02:50", "프론트엔드 마스터하기", "image3_2.png",
		LocalDateTime.of(2024, 4, 2, 13, 50), LocalDateTime.of(2024, 4, 2, 14, 50), 50, 1),
	SESSION_STAGE_3_FIXTURE_3("3부 세션3 01:50-02:50", "데이터베이스 성능 최적화", "image3_3.png",
		LocalDateTime.of(2024, 4, 2, 13, 50), LocalDateTime.of(2024, 4, 2, 14, 50), 70, 3),

	/**
	 * 4부 세션 1,2,3
	 */
	SESSION_STAGE_4_FIXTURE_1("4부 세션1 02:50-03:50", "백엔드 잘하는 법", "image4_1.png",
		LocalDateTime.of(2024, 4, 2, 14, 50), LocalDateTime.of(2024, 4, 2, 15, 50), 100, 2),
	SESSION_STAGE_4_FIXTURE_2("4부 세션2 02:50-03:50", "프론트엔드 마스터하기", "image4_2.png",
		LocalDateTime.of(2024, 4, 2, 14, 50), LocalDateTime.of(2024, 4, 2, 15, 50), 50, 1),
	SESSION_STAGE_4_FIXTURE_3("4부 세션3 02:50-03:50", "데이터베이스 성능 최적화", "image4_3.png",
		LocalDateTime.of(2024, 4, 2, 14, 50), LocalDateTime.of(2024, 4, 2, 15, 50), 70, 3);

	private final String title;
	private final String sessionImage;
	private final String summary;
	private final LocalDateTime startTime;
	private final LocalDateTime endTime;
	private final Integer standardCount;
	private final Integer audioChannel;

	SessionFixture(String title, String summary, String sessionImage, LocalDateTime startTime, LocalDateTime endTime,
		Integer standardCount,
		Integer audioChannel) {
		this.title = title;
		this.sessionImage = sessionImage;
		this.summary = summary;
		this.startTime = startTime;
		this.endTime = endTime;
		this.standardCount = standardCount;
		this.audioChannel = audioChannel;
	}

	public Session createSession() {
		return Session.builder()
			.title(title)
			.sessionImage(sessionImage)
			.summary(summary)
			.startTime(startTime)
			.endTime(endTime)
			.standardCount(standardCount)
			.audioChannel(audioChannel)
			.build();
	}
}

