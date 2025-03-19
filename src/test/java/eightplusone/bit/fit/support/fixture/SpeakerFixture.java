package eightplusone.bit.fit.support.fixture;

import eightplusone.bit.fit.domain.speaker.entity.Speaker;
import lombok.Getter;

@Getter
public enum SpeakerFixture {
	SPEAKER_FIXTURE_1("http://image1.com", "스피커1", "인공지능 전문가입니다.", SessionFixture.SESSION_STAGE_1_FIXTURE_1),
	SPEAKER_FIXTURE_2("http://image2.com", "스피커2", "뭐이런분야 전문가입니다.", SessionFixture.SESSION_STAGE_1_FIXTURE_2),
	SPEAKER_FIXTURE_6("http://image6.com", "스피커6", "굳굳굳 전문가입니다.", SessionFixture.SESSION_STAGE_1_FIXTURE_3),
	SPEAKER_FIXTURE_3("http://image3.com", "스피커3", "이야 멋진 전문가입니다.", SessionFixture.SESSION_STAGE_2_FIXTURE_1),
	SPEAKER_FIXTURE_4("http://image4.com", "스피커4", "백엔드 전문가입니다.", SessionFixture.SESSION_STAGE_2_FIXTURE_2),
	SPEAKER_FIXTURE_5("http://image5.com", "스피커5", "프론트엔드 전문가입니다.", SessionFixture.SESSION_STAGE_2_FIXTURE_3);

	private final String image;
	private final String name;
	private final String description;
	private final SessionFixture session;

	SpeakerFixture(String image, String name, String description, SessionFixture session) {
		this.image = image;
		this.name = name;
		this.description = description;
		this.session = session;
	}

	public Speaker createSpeaker() {
		return Speaker.builder()
			.image(image)
			.name(name)
			.description(description)
			.session(session.createSession())
			.build();
	}
}
