package eightplusone.bit.fit.domain.session.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import eightplusone.bit.fit.domain.session.dto.SessionListResponseDto;
import eightplusone.bit.fit.domain.session.entity.Session;
import eightplusone.bit.fit.domain.session.repository.SessionRepository;
import eightplusone.bit.fit.domain.speaker.entity.Speaker;
import eightplusone.bit.fit.domain.tag.dto.TagDto;
import eightplusone.bit.fit.domain.tag.entity.Tag;
import eightplusone.bit.fit.support.fixture.SessionFixture;
import eightplusone.bit.fit.support.fixture.SpeakerFixture;
import eightplusone.bit.fit.support.fixture.TagFixture;

class SessionServiceTest {

	@Mock
	private RedisTemplate<String, Object> redisTemplate;

	@Mock
	private HashOperations<String, Object, Object> hashOperations;

	@Mock
	private SessionRepository sessionRepository;

	@InjectMocks
	private SessionService sessionService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		when(redisTemplate.opsForHash()).thenReturn(hashOperations);
	}

	@Test
	@DisplayName("체크인을 한다.")
	void checkIn() {
		// Given
		String email = "test@gmail.com";

		// When
		sessionService.checkIn(email);

		// Then
		verify(hashOperations).put("session_user", email, "null");
	}

	@Test
	@DisplayName("체크아웃을 한다.")
	void checkOut() {
		// Given
		String email = "test@gmail.com";

		// When
		sessionService.checkOut(email);

		// Then
		verify(hashOperations).delete("session_user", email);
	}

	@Test
	@DisplayName("세션혼잡도를 전송한다.")
	void getUpdatedSessionData() {
		Session session = new Session();
		setField(session, "audioChannel", 123);
		setField(session, "standardCount", 10);

		when(sessionRepository.findByIsLiveTrue()).thenReturn(List.of(session));
		when(sessionRepository.findByAudioChannel(123)).thenReturn(Optional.of(session));

		when(hashOperations.values("session_user")).thenReturn(List.of("123", "123")); // 2명 접속

		when(redisTemplate.opsForHash()).thenReturn(hashOperations);

		Map<Integer, Map<String, Object>> result = sessionService.getUpdatedSessionData();

		assertThat(result).containsKey(123);
		assertThat(result.get(123)).containsEntry("percent", 20.0);
	}

	@Test
	@DisplayName("혼잡도가 크게 바뀔 경우 전송한다.")
	void updateAndBroadcastIfChanged() {
		// Given
		Integer audioChannel = 123;
		double percent = 100.0;
		String currentLevel = "여유"; // 기존 값
		String newLevel = "혼잡"; // 예상값 (변경됨)
		Session session = new Session();
		setField(session, "audioChannel", 123);
		setField(session, "standardCount", 5);

		when(redisTemplate.opsForHash()).thenReturn(hashOperations);
		when(hashOperations.get("session_congestion", audioChannel)).thenReturn(currentLevel);
		when(sessionRepository.findByAudioChannel(audioChannel)).thenReturn(Optional.of(session));
		when(hashOperations.values("session_user")).thenReturn(List.of("123", "123", "123", "123", "123"));

		// When
		sessionService.updateAndBroadcastIfChanged(audioChannel);

		// Then
		verify(hashOperations).put("session_congestion", audioChannel.toString(), newLevel);
		verify(redisTemplate).convertAndSend(eq("/sub/session"), argThat(message -> {
			Map<String, Object> msg = (Map<String, Object>)message;
			return msg.get("sessionId").equals(audioChannel) &&
				msg.get("percent").equals(percent) &&
				msg.get("level").equals(newLevel);
		}));
	}

	@Test
	@DisplayName("태그를 기반으로 세션 목록을 조회한다")
	void getSessionsList() {
		// given
		Pageable pageable = PageRequest.of(0, 10);

		Session session1 = SessionFixture.SESSION_STAGE_1_FIXTURE_1.createSession();
		Session session4 = SessionFixture.SESSION_STAGE_2_FIXTURE_1.createSession();

		Speaker speaker1 = SpeakerFixture.SPEAKER_FIXTURE_1.createSpeaker();
		Speaker speaker4 = SpeakerFixture.SPEAKER_FIXTURE_4.createSpeaker();

		Tag tag1 = TagFixture.TAG_FIXTURE_1.createTag();

		TagDto tagDto = TagDto.from(tag1);

		List<Object[]> mockData = new ArrayList<>();
		mockData.add(new Object[] {session1, tag1, speaker1});
		mockData.add(new Object[] {session4, tag1, speaker4});

		List<Object[]> filteredData = mockData.stream()
			.filter(data -> ((Tag)data[1]).getField().equals(tagDto.getField()))
			.toList();

		long count = filteredData.size();

		when(sessionRepository.tagFilterAndSearch(pageable, tagDto))
			.thenReturn(new PageImpl<>(filteredData, pageable, count));

		// when
		Page<SessionListResponseDto> result = sessionService.getSessionsList(pageable, tagDto);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getContent()).hasSize(2);

		List<SessionListResponseDto> content = result.getContent();

		assertAll(
			() -> assertThat(content.get(0).getTitle()).isEqualTo(session1.getTitle()),
			() -> assertThat(content.get(0).getSpeaker().getName()).isEqualTo(speaker1.getName()),
			() -> assertThat(content.get(0).getTags().getField()).isEqualTo(tag1.getField()),

			() -> assertThat(content.get(1).getTitle()).isEqualTo(session4.getTitle()),
			() -> assertThat(content.get(1).getSpeaker().getName()).isEqualTo(speaker4.getName()),
			() -> assertThat(content.get(1).getTags().getField()).isEqualTo(tag1.getField())
		);
	}

	@Test
	@DisplayName("세션 목록을 전체 조회한다")
	void getAllSessionsList() {
		// given
		Pageable pageable = PageRequest.of(0, 10);

		Session session1 = SessionFixture.SESSION_STAGE_1_FIXTURE_1.createSession();
		Session session2 = SessionFixture.SESSION_STAGE_1_FIXTURE_2.createSession();
		Session session3 = SessionFixture.SESSION_STAGE_1_FIXTURE_3.createSession();
		Session session4 = SessionFixture.SESSION_STAGE_2_FIXTURE_1.createSession();
		Session session5 = SessionFixture.SESSION_STAGE_2_FIXTURE_2.createSession();
		Session session6 = SessionFixture.SESSION_STAGE_2_FIXTURE_3.createSession();

		Speaker speaker1 = SpeakerFixture.SPEAKER_FIXTURE_1.createSpeaker();
		Speaker speaker2 = SpeakerFixture.SPEAKER_FIXTURE_2.createSpeaker();
		Speaker speaker3 = SpeakerFixture.SPEAKER_FIXTURE_3.createSpeaker();
		Speaker speaker4 = SpeakerFixture.SPEAKER_FIXTURE_4.createSpeaker();
		Speaker speaker5 = SpeakerFixture.SPEAKER_FIXTURE_5.createSpeaker();
		Speaker speaker6 = SpeakerFixture.SPEAKER_FIXTURE_6.createSpeaker();

		Tag tag1 = TagFixture.TAG_FIXTURE_1.createTag();
		Tag tag2 = TagFixture.TAG_FIXTURE_2.createTag();
		Tag tag3 = TagFixture.TAG_FIXTURE_3.createTag();
		Tag tag4 = TagFixture.TAG_FIXTURE_4.createTag();
		Tag tag5 = TagFixture.TAG_FIXTURE_5.createTag();
		Tag tag6 = TagFixture.TAG_FIXTURE_6.createTag();

		List<Object[]> mockData = new ArrayList<>();
		mockData.add(new Object[] {session1, tag1, speaker1});
		mockData.add(new Object[] {session2, tag2, speaker2});
		mockData.add(new Object[] {session3, tag3, speaker3});
		mockData.add(new Object[] {session4, tag4, speaker4});
		mockData.add(new Object[] {session5, tag5, speaker5});
		mockData.add(new Object[] {session6, tag6, speaker6});

		Page<Object[]> mockPage = new PageImpl<>(mockData, pageable, mockData.size());

		when(sessionRepository.tagFilterAndSearch(pageable, null)).thenReturn(mockPage);

		// when
		Page<SessionListResponseDto> result = sessionService.getSessionsList(pageable, null);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getContent()).hasSize(6);

		List<SessionListResponseDto> content = result.getContent();

		assertAll(
			() -> assertThat(content.get(0).getTitle()).isEqualTo(session1.getTitle()),
			() -> assertThat(content.get(0).getSpeaker().getName()).isEqualTo(speaker1.getName()),
			() -> assertThat(content.get(0).getTags().getField()).isEqualTo(tag1.getField()),

			() -> assertThat(content.get(1).getTitle()).isEqualTo(session2.getTitle()),
			() -> assertThat(content.get(1).getSpeaker().getName()).isEqualTo(speaker2.getName()),
			() -> assertThat(content.get(1).getTags().getField()).isEqualTo(tag2.getField()),

			() -> assertThat(content.get(2).getTitle()).isEqualTo(session3.getTitle()),
			() -> assertThat(content.get(2).getSpeaker().getName()).isEqualTo(speaker3.getName()),
			() -> assertThat(content.get(2).getTags().getField()).isEqualTo(tag3.getField()),

			() -> assertThat(content.get(3).getTitle()).isEqualTo(session4.getTitle()),
			() -> assertThat(content.get(3).getSpeaker().getName()).isEqualTo(speaker4.getName()),
			() -> assertThat(content.get(3).getTags().getField()).isEqualTo(tag4.getField()),

			() -> assertThat(content.get(4).getTitle()).isEqualTo(session5.getTitle()),
			() -> assertThat(content.get(4).getSpeaker().getName()).isEqualTo(speaker5.getName()),
			() -> assertThat(content.get(4).getTags().getField()).isEqualTo(tag5.getField()),

			() -> assertThat(content.get(5).getTitle()).isEqualTo(session6.getTitle()),
			() -> assertThat(content.get(5).getSpeaker().getName()).isEqualTo(speaker6.getName()),
			() -> assertThat(content.get(5).getTags().getField()).isEqualTo(tag6.getField())
		);
	}

	@Test
	@DisplayName("라이브 중인 세션을 조회한다")
	void getLiveSessions() {
		// given
		Session session1 = SessionFixture.SESSION_STAGE_1_FIXTURE_1.createSession();
		Session session2 = SessionFixture.SESSION_STAGE_1_FIXTURE_2.createSession();

		Speaker speaker1 = SpeakerFixture.SPEAKER_FIXTURE_1.createSpeaker();
		Speaker speaker2 = SpeakerFixture.SPEAKER_FIXTURE_2.createSpeaker();

		Tag tag1 = TagFixture.TAG_FIXTURE_1.createTag();
		Tag tag2 = TagFixture.TAG_FIXTURE_2.createTag();

		List<Object[]> mockData = List.of(
			new Object[] {session1, speaker1, tag1},
			new Object[] {session2, speaker2, tag2}
		);

		when(sessionRepository.findLiveSessionsWithSpeakerAndTag()).thenReturn(mockData);

		// when
		List<SessionListResponseDto> result = sessionService.getLiveSessions();

		// then
		assertThat(result).isNotNull();
		assertThat(result).hasSize(2);

		assertAll(
			() -> assertThat(result.get(0).getTitle()).isEqualTo(session1.getTitle()),
			() -> assertThat(result.get(0).getSpeaker().getName()).isEqualTo(speaker1.getName()),
			() -> assertThat(result.get(0).getTags().getField()).isEqualTo(tag1.getField()),

			() -> assertThat(result.get(1).getTitle()).isEqualTo(session2.getTitle()),
			() -> assertThat(result.get(1).getSpeaker().getName()).isEqualTo(speaker2.getName()),
			() -> assertThat(result.get(1).getTags().getField()).isEqualTo(tag2.getField())
		);
	}

	@Test
	@DisplayName("청중의 현재 오디오 채널값을 업데이트한다")
	void updateSessionUserAudioChannel() {
		// given
		String email = "test@gmail.com";
		Integer audioChannel = 1;

		// when
		sessionService.updateSessionUserAudioChannel(email, audioChannel);

		// then
		verify(hashOperations).put("session_user", email, "1");
	}

	@Test
	@DisplayName("청중의 오디오 채널값 초기화 및 세션의 라이브 상태를 false로 변경한다")
	void deleteSessionData() {
		// given
		Integer audioChannel = 1;

		Map<Object, Object> sessionUserMap = Map.of(
			"test@gmail.com", "1",
			"test2@gmail.com", "2"
		);

		when(hashOperations.entries("session_user")).thenReturn(sessionUserMap);

		// when
		sessionService.deleteSessionData(audioChannel);

		// then
		verify(hashOperations).put("session_user", "test@gmail.com", "null");
		verify(sessionRepository).updateIsLiveByAudioChannel(audioChannel, false);
	}

	@Test
	@DisplayName("세션을 라이브 상태로 변경한다")
	void activateSessionLive() {
		// give
		Integer audioChannel = 1;

		// when
		sessionService.activateSessionLive(audioChannel);

		// then
		verify(sessionRepository).updateIsLiveByAudioChannel(audioChannel, true);
	}
}
