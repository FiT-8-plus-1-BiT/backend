package eightplusone.bit.fit.domain.mysession.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
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
import eightplusone.bit.fit.domain.user.entity.User;
import eightplusone.bit.fit.domain.user.repository.UserRepository;
import eightplusone.bit.fit.support.fixture.SessionFixture;
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

	@BeforeEach
	void cleanUp() {
		mySessionRepository.deleteAllInBatch();
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

		Session session1 = sessionRepository.save(SessionFixture.SESSION_STAGE_1_FIXTURE_1.createSession());
		Session session2 = sessionRepository.save(SessionFixture.SESSION_STAGE_2_FIXTURE_2.createSession());
		Session session3 = sessionRepository.save(SessionFixture.SESSION_STAGE_3_FIXTURE_3.createSession());
		Session session4 = sessionRepository.save(SessionFixture.SESSION_STAGE_4_FIXTURE_1.createSession());

		mySessionRepository.save(MySession.register(user, session1));
		mySessionRepository.save(MySession.register(user, session2));

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMddHHmm");

		//when
		List<MySessionScheduleResponseDto> responseDtos = mySessionService.findRegisteredMySessions(
			user.getEmail());
		responseDtos.sort(Comparator.comparing(MySessionScheduleResponseDto::getSessionId));

		//then
		assertThat(responseDtos)
			.extracting(MySessionScheduleResponseDto::getSessionId)
			.containsExactly(
				session1.getSessionId(),
				session2.getSessionId(),
				session3.getSessionId(),
				session4.getSessionId()
			);

		// assertAll(
		// 	() -> assertThat(responseDtos.get(0).getSessionId()).isEqualTo(session1.getSessionId()),
		// 	() -> assertThat(responseDtos.get(0).getTitle()).isEqualTo(session1.getTitle()),
		// 	() -> assertThat(responseDtos.get(0).getSummary()).isEqualTo(session1.getSummary()),
		// 	() -> assertThat(responseDtos.get(0).getSessionImage()).isEqualTo(session1.getSessionImage()),
		// 	() -> assertThat(responseDtos.get(0).getStandardCount()).isEqualTo(session1.getStandardCount()),
		// 	() -> assertThat(responseDtos.get(0).getAudioChannel()).isEqualTo(session1.getAudioChannel()),
		// 	() -> assertThat(responseDtos.get(0).getStartTime()).isEqualTo(
		// 		session1.getStartTime().format(formatter)),
		// 	() -> assertThat(responseDtos.get(0).getEndTime()).isEqualTo(
		// 		session1.getEndTime().format(formatter)),
		// 	() -> assertThat(responseDtos.get(0).getIsMySchedule()).isTrue(),
		//
		// 	() -> assertThat(responseDtos.get(1).getSessionId()).isEqualTo(session2.getSessionId()),
		// 	() -> assertThat(responseDtos.get(1).getTitle()).isEqualTo(session2.getTitle()),
		// 	() -> assertThat(responseDtos.get(1).getSummary()).isEqualTo(session2.getSummary()),
		// 	() -> assertThat(responseDtos.get(1).getSessionImage()).isEqualTo(session2.getSessionImage()),
		// 	() -> assertThat(responseDtos.get(1).getStandardCount()).isEqualTo(session2.getStandardCount()),
		// 	() -> assertThat(responseDtos.get(1).getAudioChannel()).isEqualTo(session2.getAudioChannel()),
		// 	() -> assertThat(responseDtos.get(1).getStartTime()).isEqualTo(
		// 		session2.getStartTime().format(formatter)),
		// 	() -> assertThat(responseDtos.get(1).getEndTime()).isEqualTo(
		// 		session2.getEndTime().format(formatter)),
		// 	() -> assertThat(responseDtos.get(1).getIsMySchedule()).isTrue(),
		//
		// 	() -> assertThat(responseDtos.get(2).getSessionId()).isEqualTo(session3.getSessionId()),
		// 	() -> assertThat(responseDtos.get(2).getTitle()).isEqualTo(session3.getTitle()),
		// 	() -> assertThat(responseDtos.get(2).getSummary()).isEqualTo(session3.getSummary()),
		// 	() -> assertThat(responseDtos.get(2).getSessionImage()).isEqualTo(session3.getSessionImage()),
		// 	() -> assertThat(responseDtos.get(2).getStandardCount()).isEqualTo(session3.getStandardCount()),
		// 	() -> assertThat(responseDtos.get(2).getAudioChannel()).isEqualTo(session3.getAudioChannel()),
		// 	() -> assertThat(responseDtos.get(2).getStartTime()).isEqualTo(
		// 		session3.getStartTime().format(formatter)),
		// 	() -> assertThat(responseDtos.get(2).getEndTime()).isEqualTo(
		// 		session3.getEndTime().format(formatter)),
		// 	() -> assertThat(responseDtos.get(2).getIsMySchedule()).isFalse(),
		//
		// 	() -> assertThat(responseDtos.get(3).getSessionId()).isEqualTo(session4.getSessionId()),
		// 	() -> assertThat(responseDtos.get(3).getTitle()).isEqualTo(session4.getTitle()),
		// 	() -> assertThat(responseDtos.get(3).getSummary()).isEqualTo(session4.getSummary()),
		// 	() -> assertThat(responseDtos.get(3).getSessionImage()).isEqualTo(session4.getSessionImage()),
		// 	() -> assertThat(responseDtos.get(3).getStandardCount()).isEqualTo(session4.getStandardCount()),
		// 	() -> assertThat(responseDtos.get(3).getAudioChannel()).isEqualTo(session4.getAudioChannel()),
		// 	() -> assertThat(responseDtos.get(3).getStartTime()).isEqualTo(
		// 		session4.getStartTime().format(formatter)),
		// 	() -> assertThat(responseDtos.get(3).getEndTime()).isEqualTo(
		// 		session4.getEndTime().format(formatter)),
		// 	() -> assertThat(responseDtos.get(3).getIsMySchedule()).isFalse()
		// );
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

		Session session1 = sessionRepository.save(SessionFixture.SESSION_STAGE_1_FIXTURE_1.createSession());
		Session session2 = sessionRepository.save(SessionFixture.SESSION_STAGE_2_FIXTURE_2.createSession());
		Session session3 = sessionRepository.save(SessionFixture.SESSION_STAGE_3_FIXTURE_3.createSession());
		Session session4 = sessionRepository.save(SessionFixture.SESSION_STAGE_4_FIXTURE_1.createSession());

		mySessionRepository.save(MySession.like(user, session1));
		mySessionRepository.save(MySession.like(user, session2));
		mySessionRepository.save(MySession.like(user, session3));
		mySessionRepository.save(MySession.like(user, session4));

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMddHHmm");

		//when
		List<MySessionLikedSessionsResponseDto> responseDtos = mySessionService.findLikedMySessions(
			user.getEmail());

		//then
		assertAll(
			() -> assertThat(responseDtos.get(0).getSessionId()).isEqualTo(session1.getSessionId()),
			() -> assertThat(responseDtos.get(0).getTitle()).isEqualTo(session1.getTitle()),
			() -> assertThat(responseDtos.get(0).getSummary()).isEqualTo(session1.getSummary()),
			() -> assertThat(responseDtos.get(0).getStartTime()).isEqualTo(
				session1.getStartTime().format(formatter)),
			() -> assertThat(responseDtos.get(0).getEndTime()).isEqualTo(
				session1.getEndTime().format(formatter)),

			() -> assertThat(responseDtos.get(1).getSessionId()).isEqualTo(session2.getSessionId()),
			() -> assertThat(responseDtos.get(1).getTitle()).isEqualTo(session2.getTitle()),
			() -> assertThat(responseDtos.get(1).getSummary()).isEqualTo(session2.getSummary()),
			() -> assertThat(responseDtos.get(1).getStartTime()).isEqualTo(
				session2.getStartTime().format(formatter)),
			() -> assertThat(responseDtos.get(1).getEndTime()).isEqualTo(
				session2.getEndTime().format(formatter)),

			() -> assertThat(responseDtos.get(2).getSessionId()).isEqualTo(session3.getSessionId()),
			() -> assertThat(responseDtos.get(2).getTitle()).isEqualTo(session3.getTitle()),
			() -> assertThat(responseDtos.get(2).getSummary()).isEqualTo(session3.getSummary()),
			() -> assertThat(responseDtos.get(2).getStartTime()).isEqualTo(
				session3.getStartTime().format(formatter)),
			() -> assertThat(responseDtos.get(2).getEndTime()).isEqualTo(
				session3.getEndTime().format(formatter)),

			() -> assertThat(responseDtos.get(3).getSessionId()).isEqualTo(session4.getSessionId()),
			() -> assertThat(responseDtos.get(3).getTitle()).isEqualTo(session4.getTitle()),
			() -> assertThat(responseDtos.get(3).getSummary()).isEqualTo(session4.getSummary()),
			() -> assertThat(responseDtos.get(3).getStartTime()).isEqualTo(
				session4.getStartTime().format(formatter)),
			() -> assertThat(responseDtos.get(3).getEndTime()).isEqualTo(
				session4.getEndTime().format(formatter))
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
