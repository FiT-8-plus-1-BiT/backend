package eightplusone.bit.fit.support.fixture;

import eightplusone.bit.fit.domain.tag.entity.Tag;
import lombok.Getter;

@Getter
public enum TagFixture {
	TAG_FIXTURE_1("AI", "머신러닝", "타입1", "초급", SessionFixture.SESSION_STAGE_1_FIXTURE_1),
	TAG_FIXTURE_2("백엔드", "보안", "타입2", "중급", SessionFixture.SESSION_STAGE_1_FIXTURE_2),
	TAG_FIXTURE_3("프론트엔드", "머신러닝", "타입3", "고급", SessionFixture.SESSION_STAGE_1_FIXTURE_3),
	TAG_FIXTURE_4("AI", "머신러닝", "타입1", "초급", SessionFixture.SESSION_STAGE_2_FIXTURE_1),
	TAG_FIXTURE_5("백엔드", "보안", "타입2", "고급", SessionFixture.SESSION_STAGE_2_FIXTURE_2),
	TAG_FIXTURE_6("프론트엔드", "머신러닝", "타입3", "초급", SessionFixture.SESSION_STAGE_2_FIXTURE_3);

	private final String field;
	private final String topic;
	private final String type;
	private final String level;
	private final SessionFixture session;

	TagFixture(String field, String topic, String type, String level, SessionFixture session) {
		this.field = field;
		this.topic = topic;
		this.type = type;
		this.level = level;
		this.session = session;
	}

	public Tag createTag() {
		return Tag.builder()
			.field(field)
			.topic(topic)
			.type(type)
			.level(level)
			.session(session.createSession())
			.build();
	}
}
