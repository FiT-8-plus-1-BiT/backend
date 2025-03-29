package eightplusone.bit.fit.domain.mysession.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import eightplusone.bit.fit.domain.mysession.dto.MySessionLikedSessionsResponseDto;
import eightplusone.bit.fit.domain.mysession.dto.MySessionScheduleResponseDto;
import eightplusone.bit.fit.domain.mysession.entity.MySession;
import eightplusone.bit.fit.domain.mysession.enums.MySessionType;
import eightplusone.bit.fit.domain.mysession.repository.MySessionRepository;
import eightplusone.bit.fit.domain.session.entity.Session;
import eightplusone.bit.fit.domain.session.repository.SessionRepository;
import eightplusone.bit.fit.domain.speaker.entity.Speaker;
import eightplusone.bit.fit.domain.speaker.repository.SpeakerRepository;
import eightplusone.bit.fit.domain.user.entity.User;
import eightplusone.bit.fit.domain.user.repository.UserRepository;
import eightplusone.bit.fit.support.fixture.SessionFixture;
import eightplusone.bit.fit.support.fixture.SpeakerFixture;
import eightplusone.bit.fit.support.fixture.UserFixture;

@SpringBootTest
@Transactional
class MySessionServiceTest {

	@Autowired
	MySessionService mySessionService;
	@Autowired
	UserRepository userRepository;
	@Autowired
	SessionRepository sessionRepository;
	@Autowired
	MySessionRepository mySessionRepository;
	@Autowired
	SpeakerRepository speakerRepository;

	@BeforeEach
	void cleanUp() {
		mySessionRepository.deleteAllInBatch();
		speakerRepository.deleteAllInBatch();
		sessionRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
	}

	@Test
	@DisplayName("사용자는 강의 미리 담기를 한다")
	void register() {
		//given
		User userFixture = UserFixture.USER_FIXTURE_1.createUser();
		User user = userRepository.save(userFixture);

		Session sessionFixture = SessionFixture.SESSION_STAGE_1_FIXTURE_1.createSession();
		Session session = sessionRepository.save(sessionFixture);
		//when
		mySessionService.registerMySession(user.getEmail(), session.getSessionId());

		//then
		List<MySession> mySessions = mySessionRepository.findSessionsByUserIdAndType(user.getId(),
			MySessionType.REGISTER);
		MySession mySession = mySessions.get(0);
		assertAll(
			() -> assertThat(mySession.getType()).isEqualTo(MySessionType.REGISTER),
			() -> assertThat(mySession.getSession().getSessionId()).isEqualTo(session.getSessionId()),
			() -> assertThat(mySession.getSession().getTitle()).isEqualTo(session.getTitle()),
			() -> assertThat(mySession.getSession().getSummary()).isEqualTo(session.getSummary()),
			() -> assertThat(mySession.getSession().getStartTime()).isEqualTo(session.getStartTime()),
			() -> assertThat(mySession.getSession().getEndTime()).isEqualTo(session.getEndTime()),
			() -> assertThat(mySession.getSession().getStandardCount()).isEqualTo(session.getStandardCount()),
			() -> assertThat(mySession.getSession().getAudioChannel()).isEqualTo(session.getAudioChannel())
		);
	}

	@Test
	@DisplayName("미리 담기를 진행한 강연 정보를 조회한다")
	void getRegisteredSessions() {
		//given
		User user = userRepository.save(UserFixture.USER_FIXTURE_1.createUser());

		Speaker speakerFixture1 = SpeakerFixture.SPEAKER_FIXTURE_1.createSpeaker();
		Speaker speakerFixture2 = SpeakerFixture.SPEAKER_FIXTURE_2.createSpeaker();
		Speaker speakerFixture3 = SpeakerFixture.SPEAKER_FIXTURE_3.createSpeaker();
		Speaker speakerFixture4 = SpeakerFixture.SPEAKER_FIXTURE_4.createSpeaker();

		sessionRepository.save(speakerFixture1.getSession());
		sessionRepository.save(speakerFixture2.getSession());
		sessionRepository.save(speakerFixture3.getSession());
		sessionRepository.save(speakerFixture4.getSession());

		Speaker speaker1 = speakerRepository.save(speakerFixture1);
		Speaker speaker2 = speakerRepository.save(speakerFixture2);
		Speaker speaker3 = speakerRepository.save(speakerFixture3);
		Speaker speaker4 = speakerRepository.save(speakerFixture4);

		mySessionRepository.save(MySession.register(user, speaker1.getSession()));
		mySessionRepository.save(MySession.register(user, speaker2.getSession()));

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMddHHmm");

		//when
		List<MySessionScheduleResponseDto> responseDtos = mySessionService.findRegisteredMySessions(
			user.getEmail());

		//then
		assertAll(
			() -> assertThat(responseDtos.get(0).getSessionId()).isEqualTo(speaker1.getSession().getSessionId()),
			() -> assertThat(responseDtos.get(0).getTitle()).isEqualTo(speaker1.getSession().getTitle()),
			() -> assertThat(responseDtos.get(0).getSummary()).isEqualTo(speaker1.getSession().getSummary()),
			() -> assertThat(responseDtos.get(0).getSessionImage()).isEqualTo(speaker1.getSession().getSessionImage()),
			() -> assertThat(responseDtos.get(0).getStandardCount()).isEqualTo(
				speaker1.getSession().getStandardCount()),
			() -> assertThat(responseDtos.get(0).getAudioChannel()).isEqualTo(speaker1.getSession().getAudioChannel()),
			() -> assertThat(responseDtos.get(0).getStartTime()).isEqualTo(
				speaker1.getSession().getStartTime().format(formatter)),
			() -> assertThat(responseDtos.get(0).getEndTime()).isEqualTo(
				speaker1.getSession().getEndTime().format(formatter)),
			() -> assertThat(responseDtos.get(0).getIsMySchedule()).isTrue(),

			() -> assertThat(responseDtos.get(1).getSessionId()).isEqualTo(speaker2.getSession().getSessionId()),
			() -> assertThat(responseDtos.get(1).getTitle()).isEqualTo(speaker2.getSession().getTitle()),
			() -> assertThat(responseDtos.get(1).getSummary()).isEqualTo(speaker2.getSession().getSummary()),
			() -> assertThat(responseDtos.get(1).getSessionImage()).isEqualTo(speaker2.getSession().getSessionImage()),
			() -> assertThat(responseDtos.get(1).getStandardCount()).isEqualTo(
				speaker2.getSession().getStandardCount()),
			() -> assertThat(responseDtos.get(1).getAudioChannel()).isEqualTo(speaker2.getSession().getAudioChannel()),
			() -> assertThat(responseDtos.get(1).getStartTime()).isEqualTo(
				speaker2.getSession().getStartTime().format(formatter)),
			() -> assertThat(responseDtos.get(1).getEndTime()).isEqualTo(
				speaker2.getSession().getEndTime().format(formatter)),
			() -> assertThat(responseDtos.get(1).getIsMySchedule()).isTrue(),

			() -> assertThat(responseDtos.get(2).getSessionId()).isEqualTo(speaker3.getSession().getSessionId()),
			() -> assertThat(responseDtos.get(2).getTitle()).isEqualTo(speaker3.getSession().getTitle()),
			() -> assertThat(responseDtos.get(2).getSummary()).isEqualTo(speaker3.getSession().getSummary()),
			() -> assertThat(responseDtos.get(2).getSessionImage()).isEqualTo(speaker3.getSession().getSessionImage()),
			() -> assertThat(responseDtos.get(2).getStandardCount()).isEqualTo(
				speaker3.getSession().getStandardCount()),
			() -> assertThat(responseDtos.get(2).getAudioChannel()).isEqualTo(speaker3.getSession().getAudioChannel()),
			() -> assertThat(responseDtos.get(2).getStartTime()).isEqualTo(
				speaker3.getSession().getStartTime().format(formatter)),
			() -> assertThat(responseDtos.get(2).getEndTime()).isEqualTo(
				speaker3.getSession().getEndTime().format(formatter)),
			() -> assertThat(responseDtos.get(2).getIsMySchedule()).isFalse(),

			() -> assertThat(responseDtos.get(3).getSessionId()).isEqualTo(speaker4.getSession().getSessionId()),
			() -> assertThat(responseDtos.get(3).getTitle()).isEqualTo(speaker4.getSession().getTitle()),
			() -> assertThat(responseDtos.get(3).getSummary()).isEqualTo(speaker4.getSession().getSummary()),
			() -> assertThat(responseDtos.get(3).getSessionImage()).isEqualTo(speaker4.getSession().getSessionImage()),
			() -> assertThat(responseDtos.get(3).getStandardCount()).isEqualTo(
				speaker4.getSession().getStandardCount()),
			() -> assertThat(responseDtos.get(3).getAudioChannel()).isEqualTo(speaker4.getSession().getAudioChannel()),
			() -> assertThat(responseDtos.get(3).getStartTime()).isEqualTo(
				speaker4.getSession().getStartTime().format(formatter)),
			() -> assertThat(responseDtos.get(3).getEndTime()).isEqualTo(
				speaker4.getSession().getEndTime().format(formatter)),
			() -> assertThat(responseDtos.get(3).getIsMySchedule()).isFalse()
		);
	}

	@Test
	@DisplayName("사용자는 강의를 좋아요 담기를 한다")
	void like() {
		//given
		User userFixture = UserFixture.USER_FIXTURE_1.createUser();
		User user = userRepository.save(userFixture);

		Session sessionFixture = SessionFixture.SESSION_STAGE_1_FIXTURE_1.createSession();
		Session session = sessionRepository.save(sessionFixture);
		//when
		mySessionService.likeMySession(user.getEmail(), session.getSessionId());

		//then
		List<MySession> mySessions = mySessionRepository.findSessionsByUserIdAndType(user.getId(),
			MySessionType.LIKE);
		MySession mySession = mySessions.get(0);
		assertAll(
			() -> assertThat(mySession.getType()).isEqualTo(MySessionType.LIKE),
			() -> assertThat(mySession.getSession().getSessionId()).isEqualTo(session.getSessionId()),
			() -> assertThat(mySession.getSession().getTitle()).isEqualTo(session.getTitle()),
			() -> assertThat(mySession.getSession().getSummary()).isEqualTo(session.getSummary()),
			() -> assertThat(mySession.getSession().getStartTime()).isEqualTo(session.getStartTime()),
			() -> assertThat(mySession.getSession().getEndTime()).isEqualTo(session.getEndTime()),
			() -> assertThat(mySession.getSession().getStandardCount()).isEqualTo(session.getStandardCount()),
			() -> assertThat(mySession.getSession().getAudioChannel()).isEqualTo(session.getAudioChannel())
		);
	}

	@Test
	@DisplayName("좋아요 담기를 진행한 강연 정보를 조회한다")
	void getLikedSessions() {
		//given
		User user = userRepository.save(UserFixture.USER_FIXTURE_1.createUser());

		Speaker speakerFixture1 = SpeakerFixture.SPEAKER_FIXTURE_1.createSpeaker();
		Speaker speakerFixture2 = SpeakerFixture.SPEAKER_FIXTURE_2.createSpeaker();
		Speaker speakerFixture3 = SpeakerFixture.SPEAKER_FIXTURE_3.createSpeaker();
		Speaker speakerFixture4 = SpeakerFixture.SPEAKER_FIXTURE_4.createSpeaker();

		sessionRepository.save(speakerFixture1.getSession());
		sessionRepository.save(speakerFixture2.getSession());
		sessionRepository.save(speakerFixture3.getSession());
		sessionRepository.save(speakerFixture4.getSession());

		Speaker speaker1 = speakerRepository.save(speakerFixture1);
		Speaker speaker2 = speakerRepository.save(speakerFixture2);
		Speaker speaker3 = speakerRepository.save(speakerFixture3);
		Speaker speaker4 = speakerRepository.save(speakerFixture4);

		mySessionRepository.save(MySession.like(user, speaker1.getSession()));
		mySessionRepository.save(MySession.like(user, speaker2.getSession()));

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMddHHmm");

		//when
		List<MySessionLikedSessionsResponseDto> responseDtos = mySessionService.findLikedMySessions(
			user.getEmail());

		//then
		assertAll(
			() -> assertThat(responseDtos.get(0).getSessionId()).isEqualTo(speaker1.getSession().getSessionId()),
			() -> assertThat(responseDtos.get(0).getTitle()).isEqualTo(speaker1.getSession().getTitle()),
			() -> assertThat(responseDtos.get(0).getSummary()).isEqualTo(speaker1.getSession().getSummary()),
			() -> assertThat(responseDtos.get(0).getSpeakerName()).isEqualTo(speaker1.getName()),
			() -> assertThat(responseDtos.get(0).getSpeakerImage()).isEqualTo(speaker1.getImage()),
			() -> assertThat(responseDtos.get(0).getStartTime()).isEqualTo(
				speaker1.getSession().getStartTime().format(formatter)),
			() -> assertThat(responseDtos.get(0).getEndTime()).isEqualTo(
				speaker1.getSession().getEndTime().format(formatter)),

			() -> assertThat(responseDtos.get(1).getSessionId()).isEqualTo(speaker2.getSession().getSessionId()),
			() -> assertThat(responseDtos.get(1).getTitle()).isEqualTo(speaker2.getSession().getTitle()),
			() -> assertThat(responseDtos.get(1).getSummary()).isEqualTo(speaker2.getSession().getSummary()),
			() -> assertThat(responseDtos.get(1).getSpeakerName()).isEqualTo(speaker2.getName()),
			() -> assertThat(responseDtos.get(1).getSpeakerImage()).isEqualTo(speaker2.getImage()),
			() -> assertThat(responseDtos.get(1).getStartTime()).isEqualTo(
				speaker2.getSession().getStartTime().format(formatter)),
			() -> assertThat(responseDtos.get(1).getEndTime()).isEqualTo(
				speaker2.getSession().getEndTime().format(formatter))
		);
	}

	@Test
	@DisplayName("사용자는 강의 미리 담기를 취소한다")
	void unregister() {
		//given
		User userFixture = UserFixture.USER_FIXTURE_1.createUser();
		User user = userRepository.save(userFixture);

		Session sessionFixture = SessionFixture.SESSION_STAGE_1_FIXTURE_1.createSession();
		Session session = sessionRepository.save(sessionFixture);

		MySession mySession = mySessionRepository.save(MySession.register(user, session));

		//when
		mySessionService.unregisterMySession(user.getEmail(), session.getSessionId());

		//then
		boolean exists = mySessionRepository.existsById(mySession.getId());
		assertThat(exists).isFalse();
	}

	@Test
	@DisplayName("사용자는 좋아요로 담은 강의를 취소한다")
	void unlike() {
		//given
		User userFixture = UserFixture.USER_FIXTURE_1.createUser();
		User user = userRepository.save(userFixture);

		Session sessionFixture = SessionFixture.SESSION_STAGE_1_FIXTURE_1.createSession();
		Session session = sessionRepository.save(sessionFixture);

		MySession mySession = mySessionRepository.save(MySession.like(user, session));

		//when
		mySessionService.unlikeMySession(user.getEmail(), session.getSessionId());

		//then
		boolean exists = mySessionRepository.existsById(mySession.getId());
		assertThat(exists).isFalse();
	}
}
